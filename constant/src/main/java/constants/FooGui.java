package constants;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;

/**
 * Created by wangkang on 19/7/31.
 */
public class FooGui extends JFrame {
    private Timer timer;
    private JLabel laber;
    BufferedImage img = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g2d = img.createGraphics();
    private long lastSend = 0;
    private long lastReceive = 0;

    public FooGui(String name, Callback callback) {
        super(name);
        JFrame.setDefaultLookAndFeelDecorated(true);
        setLayout(new BorderLayout());
        setMaximumSize(new Dimension(280, 160));
        setMinimumSize(new Dimension(280, 160));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        laber = new JLabel(name, SwingConstants.CENTER);
        add(laber, BorderLayout.CENTER);
        timer = new Timer(1000, new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                long[] objects = callback.takeText();
                if (objects == null || objects.length == 0) {
                    return;
                }
                StringBuilder text = new StringBuilder("<html><br/><br/>");
                long nowSend = objects[1];
                long nowReceive = objects[2];
                String content = "客户端数量: " + objects[0] + "<br/>发送率:" + (nowSend - lastSend) + "<br/>接收率:" + (nowReceive - lastReceive) + "<br/>发送量:" + nowSend + "<br/>接收量:" + nowReceive;
                text.append(content);
                lastReceive = nowReceive;
                lastSend = nowSend;

                text.append("</html>");
                g2d.drawString(text.toString(), 10, 10);
                laber.setText(text.toString());
            }
        });
    }


    public interface Callback {
        long[] takeText();
    }

    public void doShow() {
        SwingUtilities.invokeLater(() -> {
            JPanel jPanel = new JPanel();
            jPanel.add(laber);
            this.add(jPanel);
            this.setVisible(true);
            this.setSize(280, 160);
            timer.start();
        });
    }
}
