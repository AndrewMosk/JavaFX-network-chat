package Sample;

import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;


public class TextMessage extends TextArea {
    private int maxWidth = 380;
    private int defaultWidth = 40;
    private double fontSize = 18;

    public TextMessage(String text) {
        setWrapText(true);
        setEditable(false);
        getTextAreaBounds(this, text);
        setText(text);
    }

    private void getTextAreaBounds(TextArea textArea, String message){
        Text text = new Text(message);
        text.setFont(new Font(fontSize));
        StackPane pane = new StackPane(text);
        pane.layout();
        double width = text.getLayoutBounds().getWidth();
        textArea.setPrefWidth(width + defaultWidth);
        textArea.setMaxWidth(maxWidth);
        textArea.setPrefRowCount(getTextAreaHeight(width));
        textArea.setText(message);
    }

    private int getTextAreaHeight(double width){
        int rows = (int) (width / maxWidth * 2);
        int rowLimit = 10;

        if(rows < rowLimit){
            return rows;
        } else {
            return rowLimit;
        }
    }
}
