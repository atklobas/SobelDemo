

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;


public class ImageViewer {
	public static final String DEFAULT_FILE="flowerSmall.png";
	private JFrame frame;
	private JLabel label;
	//keeps image to be displayed to user
	private BufferedImage screen;
	//keeps original image to do processing on 
	private BufferedImage raw;
	ImageIcon icon=new ImageIcon();;
	
	
	
	public ImageViewer() throws IOException{
		SwingUtilities.invokeLater(()->{
			frame=new JFrame("image viewer");
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			setPicture(DEFAULT_FILE);
			
			//create component to display image;
			icon.setImage(screen);
			label=new JLabel(icon);
			
			
			//create panel for controls
			JPanel controls=new JPanel();
			GridLayout contLayout=new GridLayout(2,4);
			controls.setLayout(contLayout);
			
			//attaches actions to buttons
			JButton id=new JButton("Identity");
			id.addActionListener((ActionEvent e)->{identity();});
			JButton blur=new JButton("Blur");
			blur.addActionListener((ActionEvent e)->{blur();});
			JButton xSobel=new JButton("xSobel");
			xSobel.addActionListener((ActionEvent e)->{xSobel();});
			JButton ySobel=new JButton("ySobel");
			ySobel.addActionListener((ActionEvent e)->{ySobel();});
			JButton sharpen=new JButton("sharpen");
			sharpen.addActionListener((ActionEvent e)->{sharpen();});
			JButton gradient=new JButton("gradient");
			gradient.addActionListener((ActionEvent e)->{gradient();});
			JButton boxBlur=new JButton("boxBlur");
			boxBlur.addActionListener((ActionEvent e)->{boxblur();});
			JButton rand=new JButton("rand");
			rand.addActionListener((ActionEvent e)->{rand();});
			
			//adds buttons to control pannel
			controls.add(id);
			controls.add(blur);
			controls.add(xSobel);
			controls.add(ySobel);
			controls.add(sharpen);
			controls.add(gradient);
			controls.add(boxBlur);
			controls.add(rand);
			
			
			//adds things to frame
			BorderLayout layout =new BorderLayout();
			frame.setLayout(layout);
			frame.add(label, BorderLayout.CENTER);
			frame.add(controls,BorderLayout.SOUTH);
			frame.add(label);
			
			//make the now prepared frame visible
			frame.pack();
			frame.setVisible(true);
		});
	}
	
	private void setPicture(String in) {
		try {
			File f=new File(in);
			if(f.exists()) {
				raw=ImageIO.read(new File(in));
			}else {
				System.err.println(in+" not found, using blank instead");
				raw=new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
		}
		
		//Keep the modified image seperate from original 
		screen=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_ARGB);
		screen.getGraphics().drawImage(raw, 0, 0, null);
		
		icon.setImage(screen);
		//draw the change
		frame.repaint();
	}

	public void convolve(int[][] kernal,double normalization,int offset,BufferedImage source,BufferedImage dest) {
		//this is a very slow way of doing this, this can be done far more efficiently in something like
		//cuda or opencl, or opengl.
		int kernalDim=kernal.length;
		int half=kernalDim/2;
		int maxWidth=source.getWidth()-half;
		int maxHeight=source.getHeight()-half;
		//it
		for(int x=half;x<maxWidth;x++) {
			for(int y=half;y<maxHeight;y++) {
				int sum=0;
				//
				for(int xx=0,px=x-half;xx<kernalDim;xx++,px++) {
					for(int yy=0,py=y-half;yy<kernalDim;yy++,py++) {
						int rgb=source.getRGB(px, py);
						int grey=((rgb&0xff)+((rgb>>8)&0xff)+((rgb>>16)&0xff))/3;
						sum +=kernal[yy][xx]*grey;
					}
				}
				
				sum*=normalization;
				sum+=offset;
				if(sum>255)sum=255;
				if(sum<0)sum=0;
				int rgb=((int)sum)|(((int)sum)<<8)|(((int)sum)<<16)|0xFF000000;
				dest.setRGB(x, y, rgb);
				
			}
			
		}
		
	}
	
	
	public void identity() {
		//does what it says on the tin
		int kernal[][]= 
			{{1}};
		convolve(kernal,1,0,raw,screen);
		frame.repaint();
	}
	
	private void rand() {
		Random rand=new Random();
		int sum=0;
		int dim=9;
		int[][] kernel=new int[dim][dim];
		for(int i=0;i<dim;i++) {
		for(int j=0;j<dim;j++) {
				int k=rand.nextInt(10)-5;//*(j-(dim/2));
				sum+=k;
				kernel[i][j]=k;
				System.out.print(k+"\t");
			}
			System.out.println("");
		} if(sum<0)sum=-sum;
		if(sum==0)sum=2;
		System.out.println();
		convolve(kernel,1./sum*.5,127,raw,screen);
		frame.repaint();
		
	}
	
	
	public void blur() {
		int kernal[][]= 
			{	{1,1,2,2,2,1,1},
				{1,3,4,5,4,3,1},
				{2,4,7,8,7,4,2},
				{2,5,8,10,8,5,2},
				{2,4,7,8,7,4,2},
				{1,3,4,5,4,3,1},
				{1,1,2,2,2,1,1},};
		int dim=kernal.length;
		int total=0;
		for(int i=0;i<dim;i++) {
			for(int j=0;j<dim;j++) {
				total+=kernal[i][j];
			}
		}
		convolve(kernal,1./total,0,raw,screen);
		frame.repaint();
		
	}
	public void boxblur() {
		
		Random rand=new Random();
		int sum=0;
		int dim=9;
		int mid=dim/2;
		int[][] kernel=new int[dim][dim];
		for(int i=0;i<dim;i++) {
			for(int j=0;j<dim;j++) {
				int k=1;
				if((i-mid)*(i-mid)+(j-mid)*(j-mid)>dim*dim/4) {
					k=0;
				}
				
				sum+=k;
				kernel[i][j]=k;
				System.out.print(k+"\t");
			}
			System.out.println("");
		}
		convolve(kernel,1./sum,0,raw,screen);
		frame.repaint();
		
		/*
		
		int kernal[][]= 
			{	{0,0,1,1,1,0,0},
				{0,1,1,1,1,1,0},
				{1,1,1,1,1,1,1},
				{1,1,1,1,1,1,1},
				{1,1,1,1,1,1,1},
				{0,1,1,1,1,1,0},
				{0,0,1,1,1,0,0},};
		convolve(kernal,1./37,0,raw,screen);
		frame.repaint();
		*/
	}
	public void xSobel() {
		BufferedImage buf=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_RGB);
		int bkernal[][]= 
			{	{1,4,7,4,1},
				{4,16,26,16,4},
				{7,26,41,26,7},
				{4,16,26,16,4},
				{1,4,7,4,1}};
		convolve(bkernal,1./273,0,raw,buf);
		int kernal[][]= 
			{	{-1,0,1},
				{-2,0,2},
				{-1,0,1}
				};
		convolve(kernal,.5,127,buf,screen);
		frame.repaint();
	}
	public void ySobel() {
		BufferedImage buf=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_RGB);
		int bkernal[][]= 
			{	{1,4,7,4,1},
				{4,16,26,16,4},
				{7,26,41,26,7},
				{4,16,26,16,4},
				{1,4,7,4,1}};
		convolve(bkernal,1./273,0,raw,buf);
		int kernal[][]= 
			{	{-1,-2,-1},
				{0,0,0},
				{1,2,1}
				};
		convolve(kernal,.5,127,buf,screen);
		frame.repaint();
	}
	public void sharpen() {
		int kernal[][]= 
			{	{0,-1,0},
				{-1,5,-1},
				{0,-1,0}
				};
		convolve(kernal,1,0,raw,screen);
		frame.repaint();
	}
	public void gradient() {
		int kernal[][]= 
			{	{1,4,7,4,1},
				{4,16,26,16,4},
				{7,26,41,26,7},
				{4,16,26,16,4},
				{1,4,7,4,1}};
		convolve(kernal,1./273,0,raw,screen);
		
		BufferedImage xbuf=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_RGB);
		BufferedImage ybuf=new BufferedImage(raw.getWidth(),raw.getHeight(),BufferedImage.TYPE_INT_RGB);
		int kernalx[][]= 
			{	{-1,0,1},
				{-2,0,2},
				{-1,0,1}
				};
		convolve(kernalx,.5,127,screen,xbuf);
		int kernaly[][]= 
			{	{-1,-2,-1},
				{0,0,0},
				{1,2,1}
				};
		convolve(kernaly,.5,127,screen,ybuf);
		
		for(int x=0;x<raw.getWidth();x++) {
			for(int y=0;y<raw.getHeight();y++) {
				int xrgb=(xbuf.getRGB(x, y)&255)-127;
				int yrgb=(ybuf.getRGB(x, y)&255)-127;
				double angle=(Math.atan2((double)xrgb, (double)yrgb)+Math.PI)/(Math.PI*2);
				int avg=(int)Math.sqrt(xrgb*xrgb+yrgb*yrgb);
				//if(avg<100)avg=0;
				//int rgb = raw.getRGB(x, y);
				float[] hsb=new float[3];
				hsb[0]=(float)angle;
				hsb[1]=(float)avg/255;
				hsb[2]=(float)avg/255;
				//hsb[2]*=hsb[2];
				screen.setRGB(x, y, Color.HSBtoRGB(hsb[0], hsb[1], hsb[2]));
				
			}
		}
		frame.repaint();
		
		
		
	}

	

	public static void main(String[] args) throws IOException{
		ImageViewer v=new ImageViewer();
		Scanner console=new Scanner(System.in);
		while(console.hasNextLine()) {
			String in=console.nextLine();
			if(in.startsWith("out ")) {
				in=in.substring(4);
				System.out.println("writing to:"+in);
				ImageIO.write(v.screen, "PNG", new File(in));
			}else {
			
			File f=new File(in);
			if(f.exists()) {
				final String toLoad=in;
				javax.swing.SwingUtilities.invokeLater(()->{v.setPicture(toLoad);});
				
			}
			}
		}
		console.close();
	}
	

}
