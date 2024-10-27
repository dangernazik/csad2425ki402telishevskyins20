package com.example.RPS_client.RPSGame;

import java.io.*;
import java.util.Objects;
import com.example.RPS_client.controller.GameController;
import com.example.RPS_client.DTO.GameDTO;
import com.google.gson.Gson;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;


public class RPSApp extends Application {
    private RPSPlayer player1;
    private RPSPlayer player2;

    private GameController gameController;

    public RPSApp() {
        try {
            gameController = new GameController(0);
        } catch (Exception ex) {
            System.err.println("Connection with server failed!");
        }
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Rock Paper Scissors Menu");

        Image logoImage = new Image(Objects.requireNonNull(RPSApp.class.getResourceAsStream("/images/Logo.png")));
        ImageView logoImageView = new ImageView(logoImage);
        logoImageView.setFitWidth(300);
        logoImageView.setPreserveRatio(true);
        logoImageView.setSmooth(true);

        MenuBar menuBar = new MenuBar();
        Menu gameMenu = new Menu("Game");
        MenuItem newGameItem = new MenuItem("New Game");
        MenuItem loadGameItem = new MenuItem("Load Game");

        gameMenu.getItems().addAll(newGameItem, loadGameItem);
        menuBar.getMenus().add(gameMenu);

        newGameItem.setOnAction(e -> showGameModeSelection());

        loadGameItem.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Load Game");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fileChooser.showOpenDialog(primaryStage);
            try {
                loadGame(file);
                startManVsManGame(primaryStage, true);
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Load Failed");
                alert.setHeaderText(null);
                alert.setContentText("Game is already played");
                alert.showAndWait();
            }
        });

        VBox vbox = new VBox(10, logoImageView, menuBar);
        Scene scene = new Scene(vbox, 300, 350);
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();
    }

    private void loadGame(File file) throws IOException {
        Gson gson = new Gson();

        // Create a class to match the JSON structure
        class GameData {
            String Player1Name;
            String Player2Name;
            String Player1Move;
            String Player2Move;
            String Winner; // This can be null if the game is ongoing
        }

        // Read the JSON data from the file
        try (Reader reader = new FileReader(file)) {
            GameData gameData = gson.fromJson(reader, GameData.class);

            // Check if the game has a winner
            if (gameData.Winner != null && !gameData.Winner.isEmpty()) {
                throw new IllegalStateException("Game is already finished");
            }

            // Initialize players with their names and moves
            player1 = new RPSPlayer(); // Use the default constructor
            player2 = new RPSPlayer(); // Use the default constructor

            player1.setName(gameData.Player1Name);
            player2.setName(gameData.Player2Name);

            String player1Move = gameData.Player1Move;
            String player2Move = gameData.Player2Move;

            if (player1Move != null && !player1Move.isEmpty()) {
                player1.setMove(RPSPlayer.Move.valueOf(player1Move));
            }

            if (player2Move != null && !player2Move.isEmpty()) {
                player2.setMove(RPSPlayer.Move.valueOf(player2Move));
            }
        }
    }

    private void showGameModeSelection() {
        Stage modeStage = new Stage();
        modeStage.setTitle("Select Game Mode");

        Button manVsManButton = new Button(RPSMode.MAN_VS_MAN.name().replace("_", " "));
        Button manVsAIButton = new Button(RPSMode.MAN_VS_AI.name().replace("_", " "));
        Button aiVsAIButton = new Button(RPSMode.AI_VS_AI.name().replace("_", " "));

        manVsManButton.setOnAction(e -> {
            player1 = new RPSPlayer();
            player2 = new RPSPlayer();
            setPlayerNicknames(RPSMode.MAN_VS_MAN);
        });

        manVsAIButton.setOnAction(e -> {
            player1 = new RPSPlayer();
            player2 = new RPSPlayer();
            setPlayerNicknames(RPSMode.MAN_VS_AI);
        });

        aiVsAIButton.setOnAction(e -> {
            player1 = new RPSPlayer();
            player2 = new RPSPlayer();
            startAiVsAiGame(modeStage);
        });

        VBox modeLayout = new VBox(10, manVsManButton, manVsAIButton, aiVsAIButton);
        Scene modeScene = new Scene(modeLayout, 300, 200);
        modeStage.setScene(modeScene);
        modeStage.show();
    }

    private void startAiVsAiGame(Stage modeStage) {
        modeStage.close();

        Stage gameStage = new Stage();
        gameStage.setTitle("AI vs AI Game");

        GridPane grid = new GridPane();
        HBox moveImages = new HBox(10);
        grid.add(moveImages, 0, 5, 3, 1);

        Label player1Label = new Label("AI 1's Move:");
        Label player1MoveLabel = new Label();
        grid.add(player1Label, 0, 0);
        grid.add(player1MoveLabel, 1, 0);

        Label player2Label = new Label("AI 2's Move:");
        Label player2MoveLabel = new Label();
        grid.add(player2Label, 0, 1);
        grid.add(player2MoveLabel, 1, 1);

        Button playButton = new Button("Play");
        grid.add(playButton, 0, 2, 3, 1);
        Label resultLabel = new Label();
        grid.add(resultLabel, 0, 3, 3, 1);

        playButton.setOnAction(e -> {
            try {
                gameController.sendModeAndMoves(RPSMode.AI_VS_AI.name(), RPSPlayer.Move.ROCK, RPSPlayer.Move.ROCK);
                GameDTO gameResponseDto = gameController.receiveResult();
                String resultText;
                if (gameResponseDto.gameResult().equals("DRAW")) {
                    resultText = "Draw";
                } else {
                    resultText = gameResponseDto.gameResult().equals("Player 1") ? "AI1" : "AI2";
                }

                String movesHistory = "AI 1 put " + gameResponseDto.player1Move().name() + ". "
                        + "AI 2 put " + gameResponseDto.player2Move().name();
                resultLabel.setText("Result: " + resultText + "\nMoves: " + movesHistory);

                moveImages.getChildren().clear();
                moveImages.getChildren().add(createMoveImage(gameResponseDto.player1Move()));
                moveImages.getChildren().add(createMoveImage(gameResponseDto.player2Move()));
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText(null);
                alert.setContentText("Server connection failed");
                alert.showAndWait();
            }
        });

        Scene gameScene = new Scene(grid, 500, 300);
        gameStage.setScene(gameScene);
        gameStage.show();

    }

    private void setPlayerNicknames(RPSMode gameMode) {
        Stage nicknameStage = new Stage();
        nicknameStage.setTitle("Set Player Nicknames");

        TextField player1NameField = new TextField();
        player1NameField.setPromptText("Enter Player 1 Name");

        TextField player2NameField = new TextField();
        player2NameField.setPromptText("Enter Player 2 Name");

        Button confirmButton = new Button("Confirm");
        confirmButton.setOnAction(e -> {
            player1.setName(player1NameField.getText());
            player2.setName(player2NameField.getText());

            if (gameMode.equals(RPSMode.MAN_VS_MAN)) {
                startManVsManGame(nicknameStage, false);
            }
            if (gameMode.equals(RPSMode.MAN_VS_AI)) {
                startManVsAiGame(nicknameStage);
            }

        });

        VBox nicknameLayout = new VBox(10, player1NameField, player2NameField, confirmButton);
        Scene nicknameScene = new Scene(nicknameLayout, 300, 200);
        nicknameStage.setScene(nicknameScene);
        nicknameStage.show();
    }

    private void startManVsAiGame(Stage nicknameStage) {
        nicknameStage.close();

        Stage gameStage = new Stage();
        gameStage.setTitle("Man vs AI Game");

        GridPane grid = new GridPane();
        HBox moveImages = new HBox(10);
        grid.add(moveImages, 0, 5, 3, 1);

        Label player1Label = new Label(player1.getName() + "'s Move:");
        ChoiceBox<RPSPlayer.Move> player1Move = new ChoiceBox<>();
        player1Move.getItems().addAll(RPSPlayer.Move.values());
        Button player1MakeMoveButton = new Button("Make Move");
        grid.add(player1Label, 0, 0);
        grid.add(player1Move, 1, 0);
        grid.add(player1MakeMoveButton, 2, 0);

        Label player2Label = new Label("AI's Move:");
        Label player2MoveLabel = new Label();
        grid.add(player2Label, 0, 1);
        grid.add(player2MoveLabel, 1, 1);

        Button playButton = new Button("Play");
        grid.add(playButton, 0, 2, 3, 1);
        Label resultLabel = new Label();
        grid.add(resultLabel, 0, 3, 3, 1);

        player1MakeMoveButton.setOnAction(e -> {
            if (player1Move.getValue() != null) {
                player1.setMove(player1Move.getValue());
                player1Move.setDisable(true);
                player1Move.getSelectionModel().clearSelection();
                resultLabel.setText(player1.getName() + " has made their move!");
            } else {
                resultLabel.setText("Player 1 must make a move!");
            }
        });

        playButton.setOnAction(e -> {
            if (player1.getMove() != null) {
                try {
                    gameController.sendModeAndMoves(RPSMode.MAN_VS_AI.name(), player1.getMove(), RPSPlayer.Move.ROCK);
                    GameDTO gameResponseDto = gameController.receiveResult();

                    String resultText;
                    if (gameResponseDto.gameResult().equals("DRAW")) {
                        resultText = "Draw";
                    } else {
                        resultText = gameResponseDto.gameResult().equals("Player 1") ? player1.getName() : "AI";
                    }

                    String movesHistory = player1.getName() + " put " + gameResponseDto.player1Move().name() + ". "
                            + player2.getName() + " put " + gameResponseDto.player2Move().name();
                    resultLabel.setText("Result: " + resultText + " wins!\n Moves: " + movesHistory);

                    moveImages.getChildren().clear();
                    moveImages.getChildren().add(createMoveImage(gameResponseDto.player1Move()));
                    moveImages.getChildren().add(createMoveImage(gameResponseDto.player2Move()));
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Connection Error");
                    alert.setHeaderText(null);
                    alert.setContentText("Server connection failed");
                    alert.showAndWait();
                }
            } else {
                resultLabel.setText("Player 1 must make a move!");
            }
        });

        Scene gameScene = new Scene(grid, 500, 300);
        gameStage.setScene(gameScene);
        gameStage.show();

    }

    private void startManVsManGame(Stage nicknameStage, boolean isLoaded) {
        nicknameStage.close();

        Stage gameStage = new Stage();
        gameStage.setTitle("Man vs Man Game");

        GridPane grid = new GridPane();
        HBox moveImages = new HBox(10);
        grid.add(moveImages, 0, 5, 3, 1);

        Label player1Label = new Label(player1.getName() + "'s Move:");
        ChoiceBox<RPSPlayer.Move> player1Move = new ChoiceBox<>();
        player1Move.getItems().addAll(RPSPlayer.Move.values());
        Button player1MakeMoveButton = new Button("Make Move");
        grid.add(player1Label, 0, 0);
        grid.add(player1Move, 1, 0);
        grid.add(player1MakeMoveButton, 2, 0);

        Label player2Label = new Label(player2.getName() + "'s Move:");
        ChoiceBox<RPSPlayer.Move> player2Move = new ChoiceBox<>();
        player2Move.getItems().addAll(RPSPlayer.Move.values());
        Button player2MakeMoveButton = new Button("Make Move");
        grid.add(player2Label, 0, 1);
        grid.add(player2Move, 1, 1);
        grid.add(player2MakeMoveButton, 2, 1);

        Button playButton = new Button("Play");
        grid.add(playButton, 0, 2, 3, 1);
        Label resultLabel = new Label();
        grid.add(resultLabel, 0, 3, 3, 1);

        Button saveButton = new Button("Save");
        grid.add(saveButton, 0, 4, 3, 1);

        if (isLoaded) {
            if (player1.getMove() != null) {
                player1Move.setDisable(true);
                player1MakeMoveButton.setDisable(true);
            }

            if (player2.getMove() != null) {
                player2Move.setDisable(true);
                player2MakeMoveButton.setDisable(true);
            }
        }

        player1MakeMoveButton.setOnAction(e -> {
            if (player1Move.getValue() != null) {
                player1.setMove(player1Move.getValue());
                player1Move.setDisable(true);
                player1Move.getSelectionModel().clearSelection();
                resultLabel.setText(player1.getName() + " has made their move!");
            } else {
                resultLabel.setText("Player 1 must make a move!");
            }
        });

        player2MakeMoveButton.setOnAction(e -> {
            if (player2Move.getValue() != null) {
                player2.setMove(player2Move.getValue());
                player2Move.setDisable(true);
                player2Move.getSelectionModel().clearSelection();
                resultLabel.setText(player2.getName() + " has made their move!");
            } else {
                resultLabel.setText("Player 2 must make a move!");
            }
        });

        String[] gameResultHolder = new String[1];
        playButton.setOnAction(e -> {
            try {
                gameController.sendModeAndMoves(RPSMode.MAN_VS_MAN.name(), player1.getMove(), player2.getMove());
                GameDTO gameResponseDto = gameController.receiveResult();

                if (gameResponseDto.gameResult().equals("DRAW")) {
                    gameResultHolder[0] = "Draw";
                } else {
                    gameResultHolder[0] = gameResponseDto.gameResult().equals("Player 1") ? player1.getName() : player2.getName();
                }

                String movesHistory = player1.getName() + " put " + gameResponseDto.player1Move().name() + ". "
                        + player2.getName() + " put " + gameResponseDto.player2Move().name();
                resultLabel.setText("Result: " + gameResultHolder[0] + " wins!\n Moves: " + movesHistory);

                moveImages.getChildren().clear();
                moveImages.getChildren().add(createMoveImage(gameResponseDto.player1Move()));
                moveImages.getChildren().add(createMoveImage(gameResponseDto.player2Move()));
            } catch (Exception ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Connection Error");
                alert.setHeaderText(null);
                alert.setContentText("Server connection failed");
                alert.showAndWait();
            }
        });

        saveButton.setOnAction(e -> {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save Game Result");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files", "*.json"));
            File file = fileChooser.showSaveDialog(gameStage);

            if (file != null) {
                try {
                    saveGameResult(file, String.valueOf(gameResultHolder[0]), player1, player2);
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Save Successful");
                    alert.setHeaderText(null);
                    alert.setContentText("Game result saved successfully!");
                    alert.showAndWait();
                } catch (IOException ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Save Failed");
                    alert.setHeaderText(null);
                    alert.setContentText("Failed to save game result: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });

        Scene gameScene = new Scene(grid, 500, 300);
        gameStage.setScene(gameScene);
        gameStage.show();
    }

    private void saveGameResult(File file, String result, RPSPlayer player1, RPSPlayer player2) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("{\n"); // Початок JSON-об'єкта
            writer.write("  \"Player1Name\": \"" + player1.getName() + "\",\n");
            writer.write("  \"Player2Name\": \"" + player2.getName() + "\",\n");
            writer.write("  \"Player1Move\": \"" + (player1.getMove() != null ? player1.getMove().name() : "") + "\",\n");
            writer.write("  \"Player2Move\": \"" + (player2.getMove() != null ? player2.getMove().name() : "") + "\",\n");
            writer.write("  \"Winner\": \"" + result + "\"\n");
            writer.write("}\n"); // Кінець JSON-об'єкта
        }
    }

    private ImageView createMoveImage(RPSPlayer.Move move) {
        String imagePath = "";

        switch (move) {
            case ROCK:
                imagePath = "/images/Rock.png";
                break;
            case PAPER:
                imagePath = "/images/Paper.png";
                break;
            case SCISSORS:
                imagePath = "/images/Scissors.png";
                break;
        }
        Image image = new Image(RPSApp.class.getResourceAsStream(imagePath));
        ImageView imageView = new ImageView(image);
        imageView.setFitWidth(100);
        imageView.setPreserveRatio(true);
        return imageView;
    }

    public static void main(String[] args) {
        launch(args);
    }
}