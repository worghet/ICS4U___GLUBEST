
import com.github.sarxos.webcam.WebcamResolution;
import com.github.sarxos.webcam.Webcam;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class tester {
    public static void main(String[] args) {
        
        // initialize & open webcam
        Webcam webcam = Webcam.getDefault();
		webcam.setViewSize(WebcamResolution.VGA.getSize());
        webcam.open();

        // make jframe
        JFrame window = new JFrame("Manual Image Test");
        window.setSize(640, 480); 
        window.setResizable(false);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setVisible(true);

        // add label with background
        JLabel backgroundLabel = new JLabel();
        backgroundLabel.setBounds(0, 0, 640, 480); 
        window.add(backgroundLabel);

        while (true) {
            try {

                // update image background with picture taken from webcam
                backgroundLabel.setIcon(new ImageIcon(webcam.getImage()));
           
                // cap at 25 fps 
                Thread.sleep(40);

            }
            catch (Exception e) {
                System.out.println("sum went wrong in rendering");
            }
        }
		

		// while
		
		// Webcam webcam = Webcam.getDefault();
		// webcam.setViewSize(WebcamResolution.VGA.getSize());

		// WebcamPanel panel = new WebcamPanel(webcam);
		// panel.setFPSDisplayed(true);
		// panel.setDisplayDebugInfo(true);
		// panel.setImageSizeDisplayed(true);
		// panel.setMirrored(true);

		// JFrame window = new JFrame("Test webcam panel");
		// window.add(panel);
		// window.setResizable(true);
		// window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		// window.pack();
		// window.setVisible(true);
    }
}
