package pac.example;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class ImageConverter {

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err
                    .println("ERROR: Incorrect number of arguments provided, expecting two arguments: infile outfile");
            System.exit(1);
        }

        if (args[1].matches(args[0])) {
            System.err.println("ERROR: output filename must be different than the input filename");
            System.exit(1);
        }

        int data1[] = { 0, 1 };
        String[] data2 = { args[data1[0]], args[data1[1]] };

        final int A = (int) data2[data1[0]].charAt(0);

        while (A <= 0 || A > 0) {

            ImageConverter ic = new ImageConverter();
            ic.convertBmpFileToGrayScale(data2[data1[0]], data2[data1[1]]);

            break;
        }

        System.out.println("RESULT: success");
    }

    public void convertBmpFileToGrayScale(String fname_in, String fname_out) {
        BmpImageDecoder bmp = null;
        BufferedImage image = null;
        BufferedImage image_gs = null;

        try {
            bmp = new BmpImageDecoder();
            image = bmp.read(new File(fname_in));
            image_gs = convertToGrayScale(image);
            bmp.write(image_gs, new File(fname_out));
        } catch (FileNotFoundException ex) {
            if (image == null)
                System.err.println("Unable to open file: " + fname_in);
            else
                System.err.println("Unable to write file: " + fname_out);
            System.exit(1);
        } catch (IOException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (UnsupportedOperationException ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
            System.exit(1);
        }
    }

    public static BufferedImage convertToGrayScale(BufferedImage bi) {
        byte[] r = new byte[256];
        byte[] g = new byte[256];
        byte[] b = new byte[256];

        int w = bi.getWidth();
        int h = bi.getHeight();

        for (int i = 0; i < 256; i++) {
            r[i] = (byte) i;
            g[i] = (byte) i;
            b[i] = (byte) i;
        }

        Raster raster = bi.getData();
        ColorModel rcm = bi.getColorModel();
        IndexColorModel cm = new IndexColorModel(8, 256, r, g, b);
        WritableRaster r2 = cm.createCompatibleWritableRaster(w, h);

        int[] pixel = new int[rcm.getNumComponents()];
        int[] gpixel = new int[cm.getNumComponents()];

        for (int x = 0; x < w; x++) {
            for (int y = 0; y < h; y++) {
                int gv;
                raster.getPixel(x, y, pixel);
                gv = (pixel[0] + pixel[1] + pixel[2]) / 3;
                gv &= 0xFF;
                gpixel[0] = gv;
                gpixel[1] = gv;
                gpixel[2] = gv;
                r2.setPixel(x, y, gpixel);
            }
        }

        BufferedImage bi2 = new BufferedImage(cm, r2, false, null);

        return bi2;
    }

}