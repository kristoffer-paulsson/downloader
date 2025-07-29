package org.example.downloader.ui;

import org.example.downloader.ConfigManager;
import org.example.downloader.InversionOfControl;
import org.example.downloader.deb.Form;
import org.example.downloader.java.JavaArchitecture;
import org.example.downloader.java.JavaDownloadEnvironment;

/**
 * A Java-specific form implementation that extends the generic Form class.
 * It is used to collect information for the JavaDownloadEnvironment.
 */
public class JavaForm extends Form {

    private JavaDownloadEnvironment jde;

    public JavaForm(JavaDownloadEnvironment jde, InversionOfControl ioc) {
        super(ioc, "Java Download Environment");
        this.jde = jde;
    }

    @Override
    protected void setupForm() {
        registerQuestion(() -> askMultipleAnswerQuestion(
                "Support for Java architectures:",
                JavaArchitecture.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.ARCH.getKey(), JavaArchitecture.UNKNOWN.getArch()),
                System.out::println
        ));

        /*// Register questions specific to the Java download environment
        registerQuestion(() -> askTextQuestion("Enter the Java version to download:", response -> {
            // Process the response, e.g., store it in a config or log it
            System.out.println("Java version entered: " + response);
        }));

        registerQuestion(() -> askTextQuestion("Enter the download URL for the Java package:", response -> {
            // Process the response, e.g., store it in a config or log it
            System.out.println("Download URL entered: " + response);
        }));*/
    }

    @Override
    protected void processForm() {
        // Process collected answers here if needed
        System.out.println("Processing Java download environment form...");
    }
}
