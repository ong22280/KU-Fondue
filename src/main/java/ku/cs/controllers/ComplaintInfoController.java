package ku.cs.controllers;

import javafx.animation.FadeTransition;
import javafx.beans.value.ChangeListener;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import ku.cs.controllers.agency.AddCategoryDialogController;
import ku.cs.datastructure.ListMap;
import ku.cs.models.Complaint;
import ku.cs.models.ComplaintList;
import ku.cs.models.User;
import ku.cs.services.ComplaintListDataSource;
import ku.cs.services.DataSource;

import java.io.IOException;

public class ComplaintInfoController {

    private User user;
    private Complaint complaint;
    private DataSource<ComplaintList> complaintListDataSource;
    private ComplaintList complaintList;

    @FXML private VBox contentContainer;
    @FXML private Label voteLabel;
    @FXML private Button voteButton;
    @FXML private Button manageComplaintButton;
    @FXML private VBox defaultContent;
    @FXML private VBox replyContent;
    @FXML private VBox editContent;
    @FXML private Label answerTeacherLabel;
    @FXML private TextArea answerTextArea;
    @FXML private CheckBox inProgressCheckbox;
    @FXML private CheckBox doneCheckbox;
    @FXML private HBox nameLabel;

    private String pageFrom;

    public void initData(User user, Complaint complaint, String pageFrom) {
        this.user = user;
        this.complaint = complaint;
        this.pageFrom = pageFrom;
        complaintListDataSource = new ComplaintListDataSource("data", "complaint.csv");
        complaintList = complaintListDataSource.readData();

        if (!pageFrom.equals("manageComplaint")) {
            manageComplaintButton.setVisible(false);
        }

        contentContainer.setSpacing(10);
        replyContent.getStyleClass().add("border-box");
        replyContent.setPadding(new Insets(10, 10, 10, 10));
        showComplaintData();

    }

    private void showComplaintData() {
        contentContainer.getChildren().clear();

        Label topic = new Label(complaint.getTopic());
        topic.getStyleClass().add("title");
        Label detail = new Label(complaint.getDetail());
        detail.setWrapText(true);
        contentContainer.getChildren().add(topic);
        Label date = new Label(complaint.getSimpleDate());
        contentContainer.getChildren().add(date);
        Label status = new Label(complaint.getStatus());
        status.getStyleClass().add("status");

        if (complaint.getStatus().equals("ดําเนินการ")) {
            status.getStyleClass().add("in-progress");
            // status.getStyleClass().remove("done");
        } else if (complaint.getStatus().equals("เสร็จสิ้น")) {
            // status.getStyleClass().remove("in-progress");
            status.getStyleClass().add("done");
        }

        contentContainer.getChildren().add(status);

        contentContainer.getChildren().add(new Label("หมวดหมู่ : " + complaint.getComplaintCategoryName()));

        voteLabel.setText(Integer.toString(complaint.getVote()));

        for (String question : complaint.getAdditionalDetail().keyList()) {

            if (question.isEmpty()) continue;
            Label qanda = new Label(question + " : " + complaint.getAdditionalDetail().get(question));

            contentContainer.getChildren().add(qanda);

        }

        contentContainer.getChildren().add(new Label("รายละเอียด"));
        contentContainer.getChildren().add(detail);

        FlowPane flowPane = new FlowPane();
        flowPane.setVgap(10);
        flowPane.setHgap(10);

        for (Image image : complaint.getImagesAnswer()) {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(Math.min(imageView.getImage().getWidth(), 720));
            imageView.setFitHeight(Math.min(imageView.getImage().getHeight(), 1024));

            flowPane.getChildren().add(imageView);
        }

        contentContainer.getChildren().add(flowPane);

        if (complaint.containUserVote(user)) {
            voteButton.getStyleClass().add("voted");
            voteLabel.getStyleClass().add("voted");
        }

        showAnswerTeacher();

    }

    private void showAnswerTeacher() {
        if (complaint.getAnswerTeacher().isEmpty()) {
            return;
        }

        nameLabel.getChildren().clear();
        defaultContent.setVisible(false);
        replyContent.setVisible(true);
        editContent.setVisible(false);
        answerTeacherLabel.setText(complaint.getAnswerTeacher());

        Label agencyNameLabel = new Label();

        agencyNameLabel.setText(complaint.getAgencyName());

        // show staff name if in manage complaint page
        if (pageFrom.equals("manageComplaint")) {
            nameLabel.getChildren().add(new Label(complaint.getTeacher().getName()));
            nameLabel.getChildren().add(new Label("|"));
        }

        nameLabel.getChildren().add(agencyNameLabel);

    }

    @FXML
    private void handleVoteButton() {
        if (complaint.addUserVote(user)) {
            voteButton.getStyleClass().add("voted");
            voteLabel.getStyleClass().add("voted");
        } else {
            voteButton.getStyleClass().remove("voted");
            voteLabel.getStyleClass().remove("voted");
        }


        voteLabel.setText(Integer.toString(complaint.getVote()));

        complaintList.updateComplaint(complaint);
        complaintListDataSource.writeData(complaintList);
    }

    @FXML
    private void handleManageComplaint() {
        defaultContent.setVisible(false);
        replyContent.setVisible(false);
        editContent.setVisible(true);

        answerTextArea.setText(complaint.getAnswerTeacher());

    }

    @FXML
    private void handleSelectInProgress() {
        inProgressCheckbox.setSelected(true);
        doneCheckbox.setSelected(false);
    }

    @FXML
    private void handleSelectDone() {
        inProgressCheckbox.setSelected(false);
        doneCheckbox.setSelected(true);
    }

    @FXML
    private void handleSendButton() {
        if (answerTextArea.getText().isEmpty() || (!inProgressCheckbox.isSelected() && !doneCheckbox.isSelected())) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("กรุณากรอกข้อมูลให้ครบ");
            alert.show();
            return;
        }

        complaint.setAnswerTeacher(answerTextArea.getText().replaceAll("\n", " "));
        complaint.setTeacher(user);

        if (inProgressCheckbox.isSelected()) {
            complaint.inProgress();
        } else {
            complaint.done();
        }

        complaintList.updateComplaint(complaint);
        complaintListDataSource.writeData(complaintList);

        showComplaintData();
    }

    @FXML
    private void handleCancelButton() {
        editContent.setVisible(false);
        defaultContent.setVisible(true);

        showAnswerTeacher();
    }

    @FXML
    private void handleReportButton(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ku/cs/view/reportDialog.fxml"));
        Parent root = loader.load();
        ReportDialogController controller = loader.getController();
        controller.initData(user, complaint);

        initDialogBox(actionEvent, root);

    }


    @FXML
    private void handleBackButton(ActionEvent actionEvent) throws IOException {
        FXMLLoader loader;
        Parent pane;
        if (pageFrom.equals("manageComplaint")) {
            loader = new FXMLLoader(getClass().getResource("/ku/cs/view/manageComplaint.fxml"));
            pane = loader.load();
            ManageComplaintController controller = loader.getController();
            controller.initData(user);
        } else if (pageFrom.equals("report")) {
            loader = new FXMLLoader(getClass().getResource("/ku/cs/view/report.fxml"));
            pane = loader.load();
            ReportController controller = loader.getController();
            controller.initData(user);
            // set tab to report complaint
            controller.setTab(1);
        } else {
            loader = new FXMLLoader(getClass().getResource("/ku/cs/view/complaint.fxml"));
            pane = loader.load();
            ComplaintDetailController controller = loader.getController();
            controller.initData(user);
        }

        BorderPane borderPane = (BorderPane) ((StackPane)((Node) actionEvent.getSource()).getScene().getRoot()).
                getChildren().get(0);
        borderPane.setCenter(pane);
    }

    public void initDialogBox(ActionEvent actionEvent, Parent root) {

        Scene scene = new Scene(root);

        Stage parentStage = (Stage) ((Node) actionEvent.getSource()).getScene().getWindow();
        StackPane stackPane = (StackPane) ((Node) actionEvent.getSource()).getScene().getRoot();
        VBox vBox = (VBox) stackPane.getChildren().get(1);

        Stage dialogBox = new Stage();

        String themeCSS = this.getClass().getResource("/ku/cs/css/themes/" + user.getTheme() + ".css").toExternalForm();
        String fontCSS = this.getClass().getResource("/ku/cs/css/fonts/" + user.getFont() + ".css").toExternalForm();
        root.getStylesheets().add(themeCSS);
        root.getStylesheets().add(fontCSS);

        dialogBox.initModality(Modality.APPLICATION_MODAL);
        dialogBox.initOwner(parentStage);
        dialogBox.initStyle(StageStyle.TRANSPARENT);
        scene.setFill(Color.TRANSPARENT);
        dialogBox.setScene(scene);
        // make dialog box close when click outside
        dialogBox.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (!isNowFocused) {
                dialogBox.close();
            }
        });

        // make dialog box always center to the screen of application
        ChangeListener<Number> widthListener = (observable, oldValue, newValue) -> {
            double stageWidth = newValue.doubleValue();
            dialogBox.setX(parentStage.getX() + parentStage.getWidth() / 2 - stageWidth / 2);
        };
        ChangeListener<Number> heightListener = (observable, oldValue, newValue) -> {
            double stageHeight = newValue.doubleValue();
            dialogBox.setY(parentStage.getY() + parentStage.getHeight() / 2 - stageHeight / 2);
        };
        dialogBox.widthProperty().addListener(widthListener);
        dialogBox.heightProperty().addListener(heightListener);
        dialogBox.setOnShown(e -> {
            dialogBox.widthProperty().removeListener(widthListener);
            dialogBox.heightProperty().removeListener(heightListener);
        });

        vBox.setVisible(true);
        // add transition when dark background appear
        FadeTransition ft = new FadeTransition(Duration.millis(500), vBox);
        ft.setFromValue(0.0);
        ft.setToValue(1.0);
        ft.play();

        dialogBox.showAndWait();

        vBox.setVisible(false);

    }
}
