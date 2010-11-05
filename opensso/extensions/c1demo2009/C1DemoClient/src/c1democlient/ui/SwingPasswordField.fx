package c1democlient.ui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javafx.ext.swing.SwingComponent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JComponent;
import javax.swing.JPasswordField;

public class SwingPasswordField extends SwingComponent {

    var passwordField: JPasswordField;

    public var text:String;

    public var columns:Number = 12.0 on replace oldValue {
        passwordField.setColumns(columns);
    };

    var external:Boolean = false;

    var triggered:String = bind text on replace oldValue{
        if(external){
            external = false;
        }
        else
        passwordField.setText(text);
    }
    public var action: function();

    public override function createJComponent():JComponent{
        passwordField = new JPasswordField();

        passwordField.setColumns(columns);

        passwordField.addActionListener(ActionListener{
            public override function actionPerformed(e:ActionEvent){
                action();
            }
        });

        passwordField.getDocument().addDocumentListener(DocumentListener{
            public override function insertUpdate( e:DocumentEvent) {
                external = true;
                text = passwordField.getText();
            }

            public override function removeUpdate( e:DocumentEvent) {
                external = true;
                text = passwordField.getText();
            }

            public override function changedUpdate( e:DocumentEvent) {
                external = true;
                text = passwordField.getText();
            }
        });

        return passwordField;
    }
}
