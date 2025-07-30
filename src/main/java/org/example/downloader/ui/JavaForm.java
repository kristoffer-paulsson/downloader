package org.example.downloader.ui;

import org.example.downloader.util.InversionOfControl;
import org.example.downloader.deb.Form;
import org.example.downloader.java.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

/**
 * A Java-specific form implementation that extends the generic Form class.
 * It is used to collect information for the JavaDownloadEnvironment.
 */
public class JavaForm extends Form {

    private JavaDownloadEnvironment jde;

    public JavaForm(JavaDownloadEnvironment jde, InversionOfControl ioc) {
        super(ioc, "Java Download Environment");
        this.jde = jde;
        try {
            jde.reload();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void setupForm() {

        registerQuestion(() -> askQuestion(
                "Enter Java download environment path",
                jde.getDownloadDir("java-downloads").toString(),
                this::validatePath,
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Support for Java architectures:",
                JavaArchitecture.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.ARCH.getKey(), JavaArchitecture.UNKNOWN.getArch()),
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which Java image type:",
                JavaImage.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.IMAGE.getKey(), JavaImage.UNKNOWN.getImage()),
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which Java implementation:",
                JavaImplementation.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.IMPLEMENTATION.getKey(), JavaImplementation.UNKNOWN.getImplementation()),
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which Java installers:",
                JavaInstaller.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.INSTALLER.getKey(), JavaInstaller.UNKNOWN.getInstaller()),
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which Java platforms:",
                JavaPlatform.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.PLATFORM.getKey(), JavaPlatform.UNKNOWN.getPlatform()),
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which Java vendors:",
                JavaVendor.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.VENDOR.getKey(), JavaVendor.UNKNOWN.getVendor()),
                System.out::println
        ));

        registerQuestion(() -> askMultipleAnswerQuestion(
                "Which Java versions:",
                JavaVersion.toStringList(),
                jde.get(JavaDownloadEnvironment.EnvironmentKey.VERSION.getKey(), JavaVersion.UNKNOWN.getVersion()),
                System.out::println
        ));
    }

    @Override
    protected void processForm() {
        List<Answer> answers = getAnswers();

        jde.setDownloadDir(Paths.get(answers.get(0).getResponse()));
        jde.set(JavaDownloadEnvironment.EnvironmentKey.ARCH.getKey(), answers.get(1).getResponse());
        jde.set(JavaDownloadEnvironment.EnvironmentKey.IMAGE.getKey(), answers.get(2).getResponse());
        jde.set(JavaDownloadEnvironment.EnvironmentKey.IMPLEMENTATION.getKey(), answers.get(3).getResponse());
        jde.set(JavaDownloadEnvironment.EnvironmentKey.INSTALLER.getKey(), answers.get(4).getResponse());
        jde.set(JavaDownloadEnvironment.EnvironmentKey.PLATFORM.getKey(), answers.get(5).getResponse());
        jde.set(JavaDownloadEnvironment.EnvironmentKey.VENDOR.getKey(), answers.get(6).getResponse());
        jde.set(JavaDownloadEnvironment.EnvironmentKey.VERSION.getKey(), answers.get(7).getResponse());

        try {
            jde.save();
            jde.reload();
            System.out.println("Saved and reloaded the environment.");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
