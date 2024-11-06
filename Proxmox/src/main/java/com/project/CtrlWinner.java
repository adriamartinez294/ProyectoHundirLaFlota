package com.project;
import java.net.URL;
import java.util.ResourceBundle;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.text.Text;

public class CtrlWinner implements Initializable {
    public String winner;

    @FXML
    public Text winnerText;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
    }

    public void setWinner(String winner){
        this.winner = winner;

        winnerText.setText("PLAYER " + winner);
    }

    @FXML
    private void backToLobby() {
        Main.restartApplication();
    }

}