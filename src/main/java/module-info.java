module com.wormsgroup.worms_re {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.wormsgroup.worms_re to javafx.fxml;
    exports com.wormsgroup.worms_re;
}