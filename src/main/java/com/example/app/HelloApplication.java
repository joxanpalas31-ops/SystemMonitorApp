package com.example.app;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

// Κεντρική κλάση εκκίνησης της εφαρμογής (JavaFX)
public class HelloApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {

        // Φόρτωση του FXML (UI layout)
        FXMLLoader fxmlLoader = new FXMLLoader(
                HelloApplication.class.getResource("hello-view.fxml")
        );

        // Δημιουργία σκηνής με αρχικές διαστάσεις
        Scene scene = new Scene(fxmlLoader.load(), 1280, 850);

        // Τίτλος παραθύρου (εμφανίζεται στη μπάρα των Windows)
        stage.setTitle("System Monitor Pro");

        // Τοποθέτηση της σκηνής στο παράθυρο (stage)
        stage.setScene(scene);

        // Ελάχιστες διαστάσεις για να μην χαλάει το layout
        stage.setMinWidth(1000);
        stage.setMinHeight(700);

        // Αποτρέπει το κλείσιμο της εφαρμογής όταν κλείσει το παράθυρο
        // (χρήσιμο όταν χρησιμοποιείται System Tray)
        Platform.setImplicitExit(false);

        // Εμφάνιση του παραθύρου
        stage.show();
    }

    public static void main(String[] args) {

        // Εκκίνηση της JavaFX εφαρμογής
        launch(args);
    }
}