package applications;

import chargen.Chargen;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class Canvas extends JPanel
{
    BufferedImage off_Image;
    Graphics g_off;

    int oldx, oldy;

    public Canvas()
    {
        clear();
//        g_off.setColor(Color.RED);
//        g_off.drawRect(10,10,20,20);
//        print (20,20, "Hallo");
    }

    public void setColor (int r, int g, int b)
    {
        g_off.setColor(new Color(r,g,b));
    }

    public void clear()
    {
        off_Image = new BufferedImage(1024, 1024, BufferedImage.TYPE_INT_ARGB);
        g_off = off_Image.createGraphics();
        repaint();
    }

    public void print(int x, int y, String txt)
    {
        new Chargen(Color.black, Color.white).printImg(off_Image, txt, x, y, 16);
        repaint();
    }

    public void circle(int x, int y, int radius)
    {
        g_off.drawOval(x, y, radius, radius);
        repaint();
    }

    public void square(int x, int y, int width)
    {
        g_off.drawRect(x, y, width, width);
        repaint();
    }

    public void line(int x1, int y1, int x2, int y2)
    {
        g_off.drawLine(x1, y1, x2, y2);
        repaint();
    }

    public void drawto(int x, int y)
    {
        g_off.drawLine(oldx, oldy, x, y);
        repaint();
        oldx = x;
        oldy = y;
    }

    public void plot(int x, int y)
    {
        g_off.drawRect(x, y, 1, 1);
        repaint();
        oldx = x;
        oldy = y;
    }


    @Override
    public void paint(Graphics g)
    {
        super.paint(g);
        g.drawImage(off_Image, 0, 0, 1024, 1024, null);
    }

    @Override
    public void update (Graphics g)
    {

    }
}
