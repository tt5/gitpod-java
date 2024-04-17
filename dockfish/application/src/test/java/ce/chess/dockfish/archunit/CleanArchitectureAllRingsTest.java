package ce.chess.dockfish.archunit;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.library.Architectures.onionArchitecture;

import ce.chess.dockfish.archunit.extension.CleanArchitecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeArchives;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.library.Architectures;

@AnalyzeClasses(packages = ArchConfig.PROJECT_PACKAGE,
    importOptions = {DoNotIncludeTests.class, DoNotIncludeArchives.class, DoNotIncludeJars.class})
public class CleanArchitectureAllRingsTest {

  @ArchTest
  void should_check_all_rings_using_onion_architecture_template(JavaClasses importedClasses) {
    Architectures.OnionArchitecture rules = onionArchitecture()
        .domainModels("..domain.event..", "..domain.model..")
        .domainServices("..domain.service..")
        .applicationServices("..application.service..")
        .adapter("adapter.common", "..adapter.common..")
        .adapter("adapter.in", "..adapter.in..")
        .adapter("adapter.out", "..adapter.out..")
        .ignoreDependency(resideInAPackage("..adapter.in.."),
            resideInAnyPackage("..adapter.common.."))
        .ignoreDependency(resideInAPackage("..adapter.out.."),
            resideInAnyPackage("..adapter.common.."))
        .ignoreDependency(resideInAPackage("..usecase.."),
            resideInAnyPackage("..domain.model..", "..domain.event.."))
        .withOptionalLayers(true);
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_all_rings_using_clean_architecture_template(JavaClasses importedClasses) {
    CleanArchitecture rule = CleanArchitecture.cleanArchitecture()
        .domainEvent("..domain.event..")
        .domainModel("..domain.model..")
        .domainService("..domain.service..")
        .applicationService("..application.service..")
        .adapterCommon("..adapter.common..")
        .adapterIn("..adapter.in..")
        .adapterOut("..adapter.out..")
        .useCaseIn("..usecase.in..")
        .useCaseOut("..usecase.out..");

    rule.check(importedClasses);
  }

}
