package ce.chess.dockfish.archunit.extension;

import static com.tngtech.archunit.core.domain.properties.HasName.Predicates.name;

import com.tngtech.archunit.PublicAPI;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import com.tngtech.archunit.library.Architectures;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

//
// Source: https://github.com/MatthiasEschhold/clean-architecture-and-flexibility-patterns
//
public final class CleanArchitecture implements ArchRule {

  private static final String DOMAIN_EVENT = "domain.event";
  private static final String DOMAIN_MODEL = "domain.model";
  private static final String DOMAIN_SERVICE = "domain.service";
  private static final String APPLICATION_SERVICE = "application.service";
  private static final String ADAPTER_OUT = "adapter.out";
  private static final String ADAPTER_COMMON = "adapter.common";
  private static final String ADAPTER_IN = "adapter.in";
  private static final String USECASE_IN = "usecase.in";
  private static final String USECASE_OUT = "usecase.out";
  private static final String DOMAIN = "domain";
  private static final String USECASE = "usecase";
  private static final String ADAPTER = "adapter";
  private static final String APPLICATION = "application";
  public static final String IGNORED_PACKAGES = "ignored.packages";
  private static final String SHARED_KERNEL_PATTERN = "shared.kernel";
  private static final String SHARED_OUTPUT_ADAPTER_PATTERN = "shared.output.adapter";
  private static final String SUPPORTING_SERVICE_PATTERN = "supporting.service";
  private static final String SERVICE_DECORATOR_PATTERN = "service.decorator";
  private static final String ADAPTER_OUT_OF_ADAPTER_OUT_USE_CASE_IN_PATTERN = "adapter.out.usecase.in";


  private Optional<String> overriddenDescription;
  private String domainEventPackageIdentifier;
  private String domainModelPackageIdentifier;
  private String domainServicePackageIdentifier;
  private String applicationServicePackageIdentifier;
  private String adapterCommonPackageIdentifier;
  private String adapterInPackageIdentifier;
  private String adapterOutPackageIdentifier;
  private String useCaseInPackageIdentifier;
  private String useCaseOutPackageIdentifier;
  private String useCasePackageIdentifier;
  private String adapterPackageIdentifier;
  private String domainPackageIdentifier;
  private String applicationPackageIdentifier;
  private Map<String, String> adapterPackageIdentifiers;
  private List<String> sharedOutputAdapter;
  private List<String> serviceDecorator;
  private List<String> supportingService;
  private boolean optionalLayers;
  private boolean allRingsDeepCheck;
  private List<IgnoredDependency> ignoredDependencies;
  private List<String> ignoredPackageIdentifiers;
  private List<String> adapterOutOfAdapterOutUseCaseInPattern;
  private List<String> sharedKernel;

  private CleanArchitecture() {
    this.domainEventPackageIdentifier = DOMAIN_EVENT;
    this.domainModelPackageIdentifier = DOMAIN_MODEL;
    this.domainServicePackageIdentifier = DOMAIN_SERVICE;
    this.applicationServicePackageIdentifier = APPLICATION_SERVICE;
    this.adapterCommonPackageIdentifier = ADAPTER_COMMON;
    this.adapterInPackageIdentifier = ADAPTER_IN;
    this.adapterOutPackageIdentifier = ADAPTER_OUT;
    this.useCaseInPackageIdentifier = USECASE_IN;
    this.useCaseOutPackageIdentifier = USECASE_OUT;
    this.useCasePackageIdentifier = USECASE;
    this.domainPackageIdentifier = DOMAIN;
    this.adapterPackageIdentifier = ADAPTER;
    this.applicationPackageIdentifier = APPLICATION;
    this.adapterPackageIdentifiers = new LinkedHashMap<>();
    this.optionalLayers = true;
    this.ignoredDependencies = new ArrayList<>();
    this.overriddenDescription = Optional.empty();
    this.ignoredPackageIdentifiers = new ArrayList<>();
    this.sharedOutputAdapter = new ArrayList<>();
    this.supportingService = new ArrayList<>();
    this.serviceDecorator = new ArrayList<>();
    this.adapterOutOfAdapterOutUseCaseInPattern = new ArrayList<>();
    this.sharedKernel = new ArrayList<>();
  }

  private CleanArchitecture(Optional<String> overriddenDescription,
                            String domainEventPackageIdentifier,
                            String domainModelPackageIdentifier,
                            String domainServicePackageIdentifier,
                            String applicationServicePackageIdentifier,
                            String adapterCommonPackageIdentifier,
                            String adapterInPackageIdentifier,
                            String adapterOutPackageIdentifier,
                            String useCaseInPackageIdentifier,
                            String useCaseOutPackageIdentifier,
                            String useCasePackageIdentifier,
                            String adapterPackageIdentifier,
                            String domainPackageIdentifier,
                            String applicationPackageIdentifier,
                            Map<String, String> adapterPackageIdentifiers,
                            boolean optionalLayers,
                            boolean allRingsDeepCheck,
                            List<IgnoredDependency> ignoredDependencies,
                            List<String> sharedOutputAdapter,
                            List<String> ignoredPackageIdentifiers,
                            List<String> serviceDecorator,
                            List<String> supportingService,
                            List<String> adapterOutOfAdapterOutUseCaseInPattern,
                            List<String> sharedKernel) {
    this();
    this.overriddenDescription = overriddenDescription;
    this.domainEventPackageIdentifier = domainEventPackageIdentifier;
    this.domainModelPackageIdentifier = domainModelPackageIdentifier;
    this.domainServicePackageIdentifier = domainServicePackageIdentifier;
    this.applicationServicePackageIdentifier = applicationServicePackageIdentifier;
    this.adapterCommonPackageIdentifier = adapterCommonPackageIdentifier;
    this.adapterInPackageIdentifier = adapterInPackageIdentifier;
    this.adapterOutPackageIdentifier = adapterOutPackageIdentifier;
    this.useCaseInPackageIdentifier = useCaseInPackageIdentifier;
    this.useCaseOutPackageIdentifier = useCaseOutPackageIdentifier;
    this.useCasePackageIdentifier = useCasePackageIdentifier;
    this.adapterPackageIdentifier = adapterPackageIdentifier;
    this.domainPackageIdentifier = domainPackageIdentifier;
    this.adapterPackageIdentifiers = adapterPackageIdentifiers;
    this.optionalLayers = optionalLayers;
    this.allRingsDeepCheck = allRingsDeepCheck;
    this.ignoredDependencies = ignoredDependencies;
    this.applicationPackageIdentifier = applicationPackageIdentifier;
    this.sharedOutputAdapter = sharedOutputAdapter;
    this.ignoredPackageIdentifiers = ignoredPackageIdentifiers;
    this.serviceDecorator = serviceDecorator;
    this.supportingService = supportingService;
    this.adapterOutOfAdapterOutUseCaseInPattern = adapterOutOfAdapterOutUseCaseInPattern;
    this.sharedKernel = sharedKernel;
  }

  public static CleanArchitecture cleanArchitecture() {
    return new CleanArchitecture();
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture domainEvent(String packageIdentifier) {
    this.domainEventPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture domainModel(String packageIdentifier) {
    this.domainModelPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture domainService(String packageIdentifier) {
    this.domainServicePackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture applicationService(String packageIdentifier) {
    this.applicationServicePackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture adapterCommon(String packageIdentifier) {
    this.adapterCommonPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture adapterIn(String packageIdentifier) {
    this.adapterInPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture adapterOut(String packageIdentifier) {
    this.adapterOutPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture useCaseIn(String packageIdentifier) {
    this.useCaseInPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture useCaseOut(String packageIdentifier) {
    this.useCaseOutPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture application(String packageIdentifier) {
    this.applicationPackageIdentifier = packageIdentifier;
    return this;
  }

  @PublicAPI(
      usage = PublicAPI.Usage.ACCESS
  )
  public CleanArchitecture supportingService(String... packageIdentifier) {
    this.supportingService = List.of(packageIdentifier);
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture serviceDecorator(String... packageIdentifer) {
    this.serviceDecorator = List.of(packageIdentifer);
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture sharedOutputAdapter(String... packageIdentifier) {
    this.sharedOutputAdapter = List.of(packageIdentifier);
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture sharedKernel(String... packageIdentifier) {
    this.sharedKernel = List.of(packageIdentifier);
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture ignore(String... packageIdentifier) {
    this.ignoredPackageIdentifiers = List.of(packageIdentifier);
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture adapterOutOfAdapterOutUseCaseInPattern(String... packageIdentifier) {
    this.adapterOutOfAdapterOutUseCaseInPattern = List.of(packageIdentifier);
    return this;
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture ignoreDependency(Class<?> origin, Class<?> target) {
    return this.ignoreDependency(JavaClass.Predicates.equivalentTo(origin), JavaClass.Predicates.equivalentTo(target));
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture ignoreDependency(String origin, String target) {
    return this.ignoreDependency(name(origin), name(target));
  }

  @PublicAPI(usage = PublicAPI.Usage.ACCESS)
  public CleanArchitecture ignoreDependency(
      DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
    this.ignoredDependencies.add(new IgnoredDependency(origin, target));
    return this;
  }

  private Architectures.LayeredArchitecture layeredArchitectureDelegate() {
    return allRingsArchitecturalExpressive();
  }

  private Architectures.LayeredArchitecture allRingsArchitecturalExpressive() {
    Architectures.LayeredArchitecture layeredArchitectureDelegate = Architectures.layeredArchitecture()
        .consideringAllDependencies()
        .optionalLayer(DOMAIN_EVENT).definedBy(this.domainEventPackageIdentifier)
        .layer(DOMAIN_MODEL).definedBy(this.domainModelPackageIdentifier)
        .layer(DOMAIN_SERVICE).definedBy(this.domainServicePackageIdentifier)
        .optionalLayer(APPLICATION_SERVICE).definedBy(this.applicationServicePackageIdentifier)
        .layer(ADAPTER_COMMON).definedBy(this.adapterCommonPackageIdentifier)
        .layer(ADAPTER_IN).definedBy(this.adapterInPackageIdentifier)
        .layer(ADAPTER_OUT).definedBy(this.adapterOutPackageIdentifier)
        .layer(USECASE_IN).definedBy(this.useCaseInPackageIdentifier)
        .layer(USECASE_OUT).definedBy(this.useCaseOutPackageIdentifier)
        .optionalLayer(IGNORED_PACKAGES).definedBy(ignoredPackageIdentifiers.toArray(new String[0]))
        .optionalLayer(SHARED_OUTPUT_ADAPTER_PATTERN).definedBy(sharedOutputAdapter.toArray(new String[0]))
        .optionalLayer(SERVICE_DECORATOR_PATTERN).definedBy(serviceDecorator.toArray(new String[0]))
        .optionalLayer(SUPPORTING_SERVICE_PATTERN).definedBy(supportingService.toArray(new String[0]))
        .optionalLayer(SHARED_KERNEL_PATTERN).definedBy(sharedKernel.toArray(new String[0]))
        .optionalLayer(ADAPTER_OUT_OF_ADAPTER_OUT_USE_CASE_IN_PATTERN)
        .definedBy(adapterOutOfAdapterOutUseCaseInPattern.toArray(new String[0]))
        .whereLayer(DOMAIN_EVENT).mayOnlyBeAccessedByLayers(
            DOMAIN_SERVICE,
            USECASE_OUT,
            ADAPTER_OUT,
            APPLICATION_SERVICE,
            IGNORED_PACKAGES,
            SHARED_OUTPUT_ADAPTER_PATTERN
        )
        .whereLayer(DOMAIN_MODEL).mayOnlyBeAccessedByLayers(
            DOMAIN_EVENT,
            DOMAIN_SERVICE,
            USECASE_OUT,
            USECASE_IN,
            ADAPTER_COMMON,
            ADAPTER_IN,
            ADAPTER_OUT,
            APPLICATION_SERVICE,
            IGNORED_PACKAGES,
            SHARED_OUTPUT_ADAPTER_PATTERN
        )
        .whereLayer(DOMAIN_SERVICE).mayOnlyBeAccessedByLayers(
            APPLICATION_SERVICE,
            SERVICE_DECORATOR_PATTERN,
            IGNORED_PACKAGES
        )
        .whereLayer(APPLICATION_SERVICE).mayOnlyBeAccessedByLayers(
            USECASE_IN,
            IGNORED_PACKAGES
        )
        .whereLayer(ADAPTER_COMMON).mayOnlyBeAccessedByLayers(
            ADAPTER_IN,
            ADAPTER_OUT
        )
        .whereLayer(ADAPTER_IN).mayOnlyBeAccessedByLayers(
            IGNORED_PACKAGES
        )
        .whereLayer(ADAPTER_OUT).mayOnlyBeAccessedByLayers(
            IGNORED_PACKAGES
        )
        .whereLayer(USECASE_IN).mayOnlyBeAccessedByLayers(
            ADAPTER_COMMON,
            ADAPTER_IN,
            APPLICATION_SERVICE,
            DOMAIN_SERVICE,
            IGNORED_PACKAGES,
            ADAPTER_OUT_OF_ADAPTER_OUT_USE_CASE_IN_PATTERN
        )
        .whereLayer(USECASE_OUT).mayOnlyBeAccessedByLayers(
            ADAPTER_COMMON,
            ADAPTER_OUT,
            DOMAIN_SERVICE,
            APPLICATION_SERVICE,
            SHARED_OUTPUT_ADAPTER_PATTERN,
            IGNORED_PACKAGES
        )
        .whereLayer(SHARED_OUTPUT_ADAPTER_PATTERN).mayOnlyBeAccessedByLayers(
            IGNORED_PACKAGES
        )
        .whereLayer(SUPPORTING_SERVICE_PATTERN).mayOnlyBeAccessedByLayers(
            APPLICATION_SERVICE,
            DOMAIN_SERVICE,
            IGNORED_PACKAGES
        )
        .whereLayer(SHARED_KERNEL_PATTERN).mayOnlyBeAccessedByLayers(
            DOMAIN_SERVICE,
            USECASE_OUT,
            USECASE_IN,
            ADAPTER_IN,
            ADAPTER_OUT,
            APPLICATION_SERVICE,
            DOMAIN_MODEL,
            IGNORED_PACKAGES
        )
        .withOptionalLayers(true);

    enrichWithIgnoreDependencies(layeredArchitectureDelegate);

    return layeredArchitectureDelegate.as(this.getDescription());
  }


  private void enrichWithIgnoreDependencies(Architectures.LayeredArchitecture layeredArchitectureDelegate) {
    IgnoredDependency ignoredDependency;
    for (var iter = this.ignoredDependencies.iterator(); iter.hasNext();
         layeredArchitectureDelegate = ignoredDependency.ignoreFor(layeredArchitectureDelegate)) {
      ignoredDependency = iter.next();
    }
  }

  @Override
  public void check(JavaClasses classes) {
    this.layeredArchitectureDelegate().check(classes);
  }

  @Override
  public ArchRule because(String reason) {
    return Factory.withBecause(this, reason);
  }

  @Override
  public ArchRule allowEmptyShould(boolean allowEmptyShould) {
    this.optionalLayers = allowEmptyShould;
    return this;
  }

  @Override
  public CleanArchitecture as(String newDescription) {
    return new CleanArchitecture(
        Optional.of(newDescription),
        domainEventPackageIdentifier,
        domainModelPackageIdentifier,
        domainServicePackageIdentifier,
        applicationServicePackageIdentifier,
        adapterCommonPackageIdentifier,
        adapterInPackageIdentifier,
        adapterOutPackageIdentifier,
        useCaseInPackageIdentifier,
        useCaseOutPackageIdentifier,
        useCasePackageIdentifier,
        adapterPackageIdentifier,
        domainPackageIdentifier,
        applicationPackageIdentifier,
        adapterPackageIdentifiers,
        optionalLayers,
        allRingsDeepCheck,
        ignoredDependencies,
        sharedOutputAdapter,
        ignoredPackageIdentifiers,
        serviceDecorator,
        supportingService,
        adapterOutOfAdapterOutUseCaseInPattern,
        sharedKernel);
  }

  @Override
  public EvaluationResult evaluate(JavaClasses classes) {
    return this.layeredArchitectureDelegate().evaluate(classes);
  }

  @Override
  public String getDescription() {
    return this.overriddenDescription
        .orElse("Clean architecture fitness function");
  }

  private static class IgnoredDependency {
    private final DescribedPredicate<? super JavaClass> origin;
    private final DescribedPredicate<? super JavaClass> target;

    IgnoredDependency(DescribedPredicate<? super JavaClass> origin, DescribedPredicate<? super JavaClass> target) {
      this.origin = origin;
      this.target = target;
    }

    Architectures.LayeredArchitecture ignoreFor(Architectures.LayeredArchitecture layeredArchitecture) {
      return layeredArchitecture.ignoreDependency(this.origin, this.target);
    }
  }

}
