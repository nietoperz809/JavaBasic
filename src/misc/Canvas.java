package misc;

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
    }

    public void setColor (int r, int g, int b)
    {
        g_off.setColor(new Color(r,g,b));
    }

    public Image getImage()
    {
        return off_Image;
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

    public void circle(int x, int y, int rad1, int rad2)
    {
        g_off.drawOval(x, y, rad1, rad2);
        repaint();
    }

    public void disc(int x, int y, int rad1, int rad2)
    {
        g_off.fillOval (x, y, rad1, rad2);
        repaint();
    }

    public void square(int x, int y, int width, int height)
    {
        g_off.drawRect(x, y, width, height);
        repaint();
    }

    public void box(int x, int y, int width, int height)
    {
        g_off.fillRect(x, y, width, height);
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
