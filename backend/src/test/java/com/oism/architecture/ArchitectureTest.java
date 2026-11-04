package com.oism.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** NFR-MAINT-01: kiem tra phu thuoc giua 4 lop Clean Architecture (chay trong CI). */
@AnalyzeClasses(packages = "com.oism", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule domainDoesNotDependOnOuterLayers = noClasses().that().resideInAPackage("com.oism.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "com.oism.web..", "com.oism.infrastructure..", "com.oism.application..");

    @ArchTest
    static final ArchRule applicationDoesNotDependOnWebOrInfrastructure =
            noClasses().that().resideInAPackage("com.oism.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("com.oism.web..", "com.oism.infrastructure..");

    @ArchTest
    static final ArchRule controllersDoNotCallRepositories = noClasses().that().resideInAPackage("com.oism.web..")
            .should().dependOnClassesThat().resideInAPackage("com.oism.domain.repository..");

    @ArchTest
    static final ArchRule webDoesNotDependOnInfrastructure = noClasses().that().resideInAPackage("com.oism.web..")
            .should().dependOnClassesThat().resideInAPackage("com.oism.infrastructure..");

    @ArchTest
    static final ArchRule servicesLiveInApplication = classes().that().areAnnotatedWith(org.springframework.stereotype.Service.class)
            .should().resideInAPackage("com.oism.application..");

    @ArchTest
    static final ArchRule restControllersLiveInWeb = classes().that()
            .areAnnotatedWith(org.springframework.web.bind.annotation.RestController.class)
            .should().resideInAPackage("com.oism.web..");
}
