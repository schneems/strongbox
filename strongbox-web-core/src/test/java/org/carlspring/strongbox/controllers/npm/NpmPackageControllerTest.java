package org.carlspring.strongbox.controllers.npm;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.given;
import static org.hamcrest.CoreMatchers.equalTo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;

import org.carlspring.strongbox.artifact.coordinates.NpmArtifactCoordinates;
import org.carlspring.strongbox.artifact.generator.NpmPackageGenerator;
import org.carlspring.strongbox.configuration.ConfigurationManager;
import org.carlspring.strongbox.controllers.context.IntegrationTest;
import org.carlspring.strongbox.storage.repository.Repository;
import org.carlspring.strongbox.storage.repository.RepositoryPolicyEnum;
import org.carlspring.strongbox.testing.TestCaseWithRepositoryManagement;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@IntegrationTest
@RunWith(SpringJUnit4ClassRunner.class)
public class NpmPackageControllerTest extends TestCaseWithRepositoryManagement
{

    private static final String STORAGE_ID = "storage-common-npm";
    private static final String REPOSITORY_RELEASES_1 = "npm-releases-test";

    @Inject
    private ConfigurationManager configurationManager;

    @Inject
    @Qualifier("contextBaseUrl")
    private String contextBaseUrl;

    @BeforeClass
    public static void cleanUp()
        throws Exception
    {
        cleanUp(getRepositoriesToClean());
    }

    public static Set<Repository> getRepositoriesToClean()
    {
        Set<Repository> repositories = new LinkedHashSet<>();
        repositories.add(createRepositoryMock(STORAGE_ID, REPOSITORY_RELEASES_1));

        return repositories;
    }

    @Before
    public void init()
        throws Exception
    {

        createStorage(STORAGE_ID);

        Repository repository1 = new Repository(REPOSITORY_RELEASES_1);
        repository1.setPolicy(RepositoryPolicyEnum.RELEASE.getPolicy());
        repository1.setStorage(configurationManager.getConfiguration().getStorage(STORAGE_ID));
        repository1.setLayout("npm");

        createRepository(repository1);
    }

    @Test
    public void testPackageCommonFlow()
        throws Exception
    {
        NpmArtifactCoordinates coordinates = NpmArtifactCoordinates.of("@carlspring/npm-test-release",
                                                                       "1.0.0");
        NpmPackageGenerator packageGenerator = NpmPackageGenerator.newInstance();
        Path publishJsonPath = packageGenerator.of(coordinates)
                                               .buildPublishJson();
        Path packagePath = packageGenerator.getPackagePath();

        byte[] publishJsonContent = Files.readAllBytes(publishJsonPath);

        given().header("User-Agent", "npm/*")
               .header("Content-Type", "application/json")
               .body(publishJsonContent)
               .when()
               .put(contextBaseUrl + "/storages/" + STORAGE_ID + "/" + REPOSITORY_RELEASES_1 + "/"
                       + coordinates.getId())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value());

        given().header("User-Agent", "npm/*")
               .header("Content-Type", "application/json")
               .when()
               .get(contextBaseUrl + "/storages/" + STORAGE_ID + "/" + REPOSITORY_RELEASES_1 + "/"
                       + coordinates.toResource())
               .peek()
               .then()
               .statusCode(HttpStatus.OK.value())
               .assertThat()
               .header("Content-Length", equalTo(String.valueOf(Files.size(packagePath))));

    }

}
