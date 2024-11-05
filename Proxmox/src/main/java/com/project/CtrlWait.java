package com.project;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;

public class CtrlWait implements Initializable {

    @FXML
    public Label txtTitle;

    @FXML
    public Label txtPlayer0;

    @FXML
    public Label txtPlayer1;

    @FXML
    public ImageView gifCargando;

    @Override
    public void initialize(URL url, ResourceBundle rb) {

    }

    public void resetWaitState() {
        txtPlayer0.setText("?");
        txtPlayer1.setText("?");
        txtTitle.setText("Waiting for players");
        gifCargando.setVisible(true);
    }
}