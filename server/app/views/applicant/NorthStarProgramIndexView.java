package views.applicant;

import static com.google.common.base.Preconditions.checkNotNull;
import static services.applicant.ApplicantPersonalInfo.ApplicantType.GUEST;

import annotations.BindingAnnotations;
import auth.CiviFormProfile;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import controllers.AssetsFinder;
import controllers.applicant.ApplicantRoutes;
import controllers.routes;
import java.util.Optional;
import modules.ThymeleafModule;
import org.thymeleaf.TemplateEngine;
import play.i18n.Messages;
import play.mvc.Http.Request;
import services.MessageKey;
import services.applicant.ApplicantPersonalInfo;
import services.applicant.ApplicantService;
import services.settings.SettingsManifest;
import views.applicant.ProgramCardsSectionParamsFactory.ProgramSectionParams;

/** Renders a list of programs that an applicant can browse, with buttons for applying. */
public class NorthStarProgramIndexView extends NorthStarApplicantBaseView {
  private final ProgramCardsSectionParamsFactory programCardsSectionParamsFactory;
  private final SettingsManifest settingsManifest;
  private final String authProviderName;

  @Inject
  NorthStarProgramIndexView(
      TemplateEngine templateEngine,
      ThymeleafModule.PlayThymeleafContextFactory playThymeleafContextFactory,
      AssetsFinder assetsFinder,
      ApplicantRoutes applicantRoutes,
      ProgramCardsSectionParamsFactory programCardsSectionParamsFactory,
      SettingsManifest settingsManifest,
      @BindingAnnotations.ApplicantAuthProviderName String authProviderName) {
    super(templateEngine, playThymeleafContextFactory, assetsFinder, applicantRoutes);
    this.programCardsSectionParamsFactory = checkNotNull(programCardsSectionParamsFactory);
    this.settingsManifest = checkNotNull(settingsManifest);
    this.authProviderName = checkNotNull(authProviderName);
  }

  public String render(
      Messages messages,
      Request request,
      long applicantId,
      ApplicantPersonalInfo personalInfo,
      ApplicantService.ApplicationPrograms applicationPrograms,
      CiviFormProfile profile) {
    ThymeleafModule.PlayThymeleafContext context = createThymeleafContext(request);

    ImmutableList.Builder<ProgramSectionParams> sectionParamsBuilder = ImmutableList.builder();

    if (!applicationPrograms.inProgress().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_IN_PROGRESS_UPDATED),
              MessageKey.BUTTON_CONTINUE,
              applicationPrograms.inProgress(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId));
    }

    if (!applicationPrograms.submitted().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_SUBMITTED),
              MessageKey.BUTTON_EDIT,
              applicationPrograms.submitted(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId));
    }

    if (!applicationPrograms.unapplied().isEmpty()) {
      sectionParamsBuilder.add(
          programCardsSectionParamsFactory.getSection(
              messages,
              Optional.of(MessageKey.TITLE_PROGRAMS_ACTIVE_UPDATED),
              MessageKey.BUTTON_APPLY,
              applicationPrograms.unapplied(),
              /* preferredLocale= */ messages.lang().toLocale(),
              profile,
              applicantId));
    }

    context.setVariable(
        "civicEntityShortName", settingsManifest.getWhitelabelCivicEntityShortName(request).get());
    context.setVariable("sections", sectionParamsBuilder.build());
    context.setVariable("authProviderName", authProviderName);
    context.setVariable("loginLink", routes.LoginController.applicantLogin(Optional.empty()).url());
    context.setVariable("createAccountLink", routes.LoginController.register().url());
    context.setVariable("isGuest", personalInfo.getType() == GUEST);

    return templateEngine.process("applicant/ProgramIndexTemplate", context);
  }
}