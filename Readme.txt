cdAll .owl files in resources folfder are replaced by the 
.owl files in ResourceAccessProxy project.


2) In AuthenticationAuthorizationManager folder the following command executed:

./Gradle.bat (= gradle assemble --refresh-dependencies)

Then the AuthenticationAuthorizationManager-5.0.12-run.jar produced in build/libs folder.

NOTICE THAT  AuthenticationAuthorizationManager compiled with Java 15 (instead of 8).

///3) NO ANYMORE //////  The SymbioteSmartSpace jar was build in Turbox laptop because needs Java 8.

////////In Turbox the JAVA_HOME changed to jdk 8, then the SmartSpaceMiddleware-3.0.0.jar build.
////The JAVA HOME is back again to JDK 7

14-01-2022
----------------------------------------
1)To build the SmartSpaceMiddleware-3.0.0.jar , run gradle.bat build inside
C:\projects\SHAPES\SSP\develop\opt\symbiote\SymbioteSmartSpace-modified folder

The gradle is not the Gradle.bat of C:\projects\SHAPES\SSP\develop\opt\symbiote\SymbioteSmartSpace-modified folder.

2) In case we use the original AAM then the port of AAM in SSP application.properties AAM bootstrap.properties, docker-compose files and 
 nginx-prod.conf must be 8081. 