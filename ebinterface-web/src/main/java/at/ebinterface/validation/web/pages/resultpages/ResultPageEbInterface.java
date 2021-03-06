package at.ebinterface.validation.web.pages.resultpages;

import at.ebinterface.validation.dto.SignatureValidationResult;
import at.ebinterface.validation.validator.ValidationResult;
import at.ebinterface.validation.validator.jaxb.Result;
import at.ebinterface.validation.web.pages.BasePage;
import at.ebinterface.validation.web.pages.StartPage;
import at.ebinterface.validation.web.pages.StartPage.ActionType;
import at.ebinterface.validation.web.panels.ErrorDetailsPanel;
import at.ebinterface.validation.web.panels.SignatureDetailsPanel;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.util.resource.AbstractResourceStreamWriter;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Used to show the results of a validation
 *
 * @author pl
 */
public class ResultPageEbInterface extends BasePage {

  /**
   * Create a new result page
   */
  public ResultPageEbInterface(final ValidationResult validationResult,
                               final String selectedSchematron,
                               final ActionType selectedAction,
                               final byte[] pdf,
                               final byte[] xml,
                               final String log) {

    final StringBuffer schemaVersion = new StringBuffer();

    if (validationResult.getDeterminedEbInterfaceVersion() != null) {
      schemaVersion.append(validationResult.getDeterminedEbInterfaceVersion().getCaption());
      if (validationResult.getDeterminedEbInterfaceVersion().isSigned()) {
        schemaVersion.append(" (signiert)");
      } else {
        schemaVersion.append(" (unsigniert)");
      }
    }

    if(schemaVersion.length() > 0) {
      //Add a label with the schema version
      add(new Label("schemaVersion", Model.of(schemaVersion.toString())));
    } else {
      add(new Label("schemaVersion", Model.of("Es wurde keine gültige Version erkannt.")));
    }

    //Schema OK Container
    final WebMarkupContainer schemaOkContainer = new WebMarkupContainer("schemvalidationOK");
    add(schemaOkContainer);

    //Schema NOK Container
    final WebMarkupContainer schemaNOkContainer = new WebMarkupContainer("schemvalidationNOK");
    schemaNOkContainer.add(new Label("schemaValidationError",
                                     Model.of(validationResult.getSchemaValidationErrorMessage())));
    add(schemaNOkContainer);


    //Schema is OK
    if (StringUtils.isEmpty(validationResult.getSchemaValidationErrorMessage())) {
      schemaOkContainer.setVisible(true);
      schemaNOkContainer.setVisible(false);
    }
    //Schema NOK
    else {
      schemaOkContainer.setVisible(false);
      schemaNOkContainer.setVisible(true);
    }

    //Signature result container
    WebMarkupContainer
        signatureResultContainer =
        new WebMarkupContainer("signatureResultContainer");
    //If no signature is applied we do not show the containers
    if (validationResult.getDeterminedEbInterfaceVersion() == null || !validationResult
        .getDeterminedEbInterfaceVersion().isSigned()) {
      signatureResultContainer.setVisibilityAllowed(false);
    }

    //Get the result details for the signature
    SignatureValidationResult
        signatureValidationResult =
        new SignatureValidationResult(validationResult.getVerifyDocumentResponse());

    //Signature
    signatureResultContainer.add(new SignatureDetailsPanel("signatureDetails", Model
        .of(signatureValidationResult.getSignatureText()), Model.of(signatureValidationResult
                                                                        .isSignatureValid())));

    //Certificate
    signatureResultContainer.add(new SignatureDetailsPanel("certificateDetails", Model
        .of(signatureValidationResult.getCertificateText()), Model.of(signatureValidationResult
                                                                          .isCertificateValid())));

    //Manifest
    signatureResultContainer.add(new SignatureDetailsPanel("manifestDetails", Model
        .of(signatureValidationResult.getManifestText()),
                                                           Model.of(signatureValidationResult
                                                                        .isManifestValid())));

    add(signatureResultContainer);

    //Schematron OK Container
    final WebMarkupContainer schematronOkContainer = new WebMarkupContainer("schematronOK");

    //Add a label with the selected Schematron
    schematronOkContainer.add(new Label("selectedSchematron", Model.of(selectedSchematron)));
    add(schematronOkContainer);

    //Schematron NOK Container
    final WebMarkupContainer schematronNokContainer = new WebMarkupContainer("schematronNOK");

    schematronNokContainer.add(new Label("selectedSchematron", Model.of(selectedSchematron)));

    final Result schematronResult = validationResult.getResult();

    //Add schematron error messages if there some
    if (schematronResult == null || schematronResult.getErrors() == null
        || schematronResult.getErrors().size() == 0) {
      schematronNokContainer.add(new EmptyPanel("errorDetailsPanel"));
    } else {
      schematronNokContainer
          .add(new ErrorDetailsPanel("errorDetailsPanel", schematronResult.getErrors()));
    }

    add(schematronNokContainer);

    //In case the Schema validation failed, or only schema validation is turned on we do not show anything about the schematron
    if (selectedAction == ActionType.SCHEMA_VALIDATION || !StringUtils
        .isEmpty(validationResult.getSchemaValidationErrorMessage())) {
      schematronOkContainer.setVisible(false);
      schematronNokContainer.setVisible(false);
    }
    //Are there schematron validation messages?
    //Everything OK
    else if (schematronResult == null || schematronResult.getErrors() == null
             || schematronResult.getErrors().size() == 0) {
      schematronOkContainer.setVisible(true);
      schematronNokContainer.setVisible(false);
    }
    //NOK
    else {
      schematronOkContainer.setVisible(false);
      schematronNokContainer.setVisible(true);
    }

    //Log Container
    final WebMarkupContainer mappingContainer = new WebMarkupContainer("mappingLog");
    add(mappingContainer);

    final WebMarkupContainer errorContainer = new WebMarkupContainer("mappingLogError");
    mappingContainer.add(errorContainer);
    errorContainer.setVisible(true);

    if (log != null){
      mappingContainer.setVisible(true);

      Label slog = new Label("logErrorPanel", Model.of(new String(log).trim()));
      errorContainer.add(slog.setEscapeModelStrings(false));
    } else {
      mappingContainer.setVisible(false);

      EmptyPanel slog = new EmptyPanel("logErrorPanel");
      errorContainer.add(slog);
    }

    add(new Link<Object>("returnLink") {
      @Override
      public void onClick() {
        setResponsePage(StartPage.class);
      }
    });

    Link<Void> pdflink = new Link<Void>("linkPDFDownload") {
      @Override
      public void onClick() {
        AbstractResourceStreamWriter rstream = new AbstractResourceStreamWriter() {
          @Override
          public void write(OutputStream output) throws IOException {
            output.write(pdf);
          }
        };

        ResourceStreamRequestHandler
            handler = new ResourceStreamRequestHandler(rstream, "ebInterface-Invoice.pdf");
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
      }
    };
    pdflink.setVisible(pdf != null);
    //Add a PDF-download button
    add(pdflink);

    Link<Void> xmllink = new Link<Void>("linkXMLDownload") {
      @Override
      public void onClick() {
        AbstractResourceStreamWriter rstream = new AbstractResourceStreamWriter() {
          @Override
          public void write(OutputStream output) throws IOException {
            output.write(xml);
          }
        };

        ResourceStreamRequestHandler
            handler = new ResourceStreamRequestHandler(rstream, "ebInterface-Invoice.xml");
        getRequestCycle().scheduleRequestHandlerAfterCurrent(handler);
      }
    };
    xmllink.setVisible(xml != null);
    //Add a PDF-download button
    add(xmllink);
  }

}

