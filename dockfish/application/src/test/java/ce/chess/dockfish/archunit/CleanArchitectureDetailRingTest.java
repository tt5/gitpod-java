package ce.chess.dockfish.archunit;

import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAnyPackage;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.simpleNameContaining;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeArchives;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeJars;
import com.tngtech.archunit.core.importer.ImportOption.DoNotIncludeTests;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.dependencies.SliceRule;

@AnalyzeClasses(packages = ArchConfig.PROJECT_PACKAGE,
    importOptions = {DoNotIncludeTests.class, DoNotIncludeArchives.class, DoNotIncludeJars.class})
public class CleanArchitectureDetailRingTest {

  @ArchTest
  void should_check_domain_event_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..domain.event..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..domain.event..", "..domain.model..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.DOMAIN_EVENT_PERMISSIONS))
        );
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_domain_model_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..domain.model..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..domain.model..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.DOMAIN_MODEL_PERMISSIONS))
        );
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_domain_service_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..domain.service..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..domain..", "..usecase.out..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.DOMAIN_SERVICE_PERMISSIONS))
            .or(simpleNameContaining("RequeueException"))
        );
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_domain_free_of_cycles(JavaClasses importedClasses) {
    SliceRule sliceRule = slices()
        .matching(ArchConfig.PROJECT_PACKAGE + "..domain.service.(*)..")
        .namingSlices("Domain Service $1")
        .should().beFreeOfCycles();
    sliceRule.check(importedClasses);
  }

  @ArchTest
  void should_check_adapter_in_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..adapter.in..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..adapter.in..", "..adapter.common..",
            "..usecase.in..", "..domain.model..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.ADAPTER_IN_PERMISSIONS))
        );
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_adapter_out_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..adapter.out..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..adapter.out..", "..adapter.common..",
            "..domain.model..", "..domain.event..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.ADAPTER_OUT_PERMISSIONS))
        );
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_adapters_not_depend(JavaClasses importedClasses) {
    SliceRule sliceRule = slices().matching(ArchConfig.PROJECT_PACKAGE + ".adapter.*.(*)..")
        .namingSlices("Adapter $1")
        .should().notDependOnEachOther()
        .ignoreDependency(resideInAnyPackage("..adapter.common.mapper.."),
            resideInAnyPackage("..adapter.common.*.."))
        .ignoreDependency(resideInAnyPackage("..adapter.in.web.."),
            resideInAnyPackage("..adapter.in.*..", "..adapter.common.*.."))
        .ignoreDependency(resideInAnyPackage("..adapter.in.rabbit.."),
            resideInAnyPackage("..adapter.in.*..", "..adapter.common.*.."))
        .ignoreDependency(resideInAnyPackage("..adapter.out.engine.."),
            resideInAnyPackage("..adapter.common.chess.."))
        .ignoreDependency(resideInAnyPackage("..adapter.out.rabbit.."),
            resideInAnyPackage("..adapter.common.*.."));
    sliceRule.check(importedClasses);
  }

  @ArchTest
  void should_check_usecase_in_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..usecase.in..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..domain.model..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.USECASE_IN_PERMISSIONS))
        );
    rules.check(importedClasses);
  }

  @ArchTest
  void should_check_usecase_out_ring(JavaClasses importedClasses) {
    ArchRule rules = classes()
        .that()
        .resideInAPackage("..usecase.out..")
        .should()
        .onlyAccessClassesThat(resideInAnyPackage("..domain.model..")
            .or(resideInAnyPackage("java..", "org.apache.logging.."))
            .or(resideInAnyPackage(ArchConfig.USECASE_OUT_PERMISSIONS))
        );
    rules.check(importedClasses);
  }
}
