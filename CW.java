import java.io.DataInputStream;
import java.io.FileInputStream;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.lang.Math;
import java.awt.Color;

class Volume {
    int data[][][];
    float zoom = 1;
    int resolution = 512;

    /**
     * This function reads a volume dataset from disk and put the result in the data array
     * <p>
     * param amplification allows increasing the brightness of the slice by a constant.
     */
    public int GetResolution() {
        return resolution;
    }

    public void SetResolution(int res) {
        resolution = res;
    }

    public float GetZoom() {
        return zoom;
    }

    public void SetZoom(float z) {
        zoom = z;
    }

    boolean ReadData(String fileName, int sizeX, int sizeY, int sizeZ, int headerSize) {
        int cpt = 0;
        byte dataBytes[] = new byte[sizeX * sizeY * sizeZ + headerSize];
        data = new int[sizeZ][sizeY][sizeX];
        try {
            FileInputStream f = new FileInputStream(fileName);
            DataInputStream d = new DataInputStream(f);

            d.readFully(dataBytes);

            //Copying the byte values into the floating-point array

            for (int k = 0; k < sizeZ; k++)
                for (int j = 0; j < sizeY; j++)
                    for (int i = 0; i < sizeX; i++)

                        // 256???
                        data[k][j][i] = dataBytes[k * sizeX * sizeY + j * sizeX + i + headerSize] & 0xff;
        } catch (Exception e) {
            System.out.println("Exception : " + cpt + e);
            return false;
        }
        return true;
    }

//-------------------------------------------------------------------------------------------------------------------

    /**
     * This function returns the 3D gradient for the volumetric dataset (data variable). Note that the gradient values at the sides of the volume is not be computable. Each cell element containing a 3D vector, the result is therefore a 4D array.
     */
    int[][][][] Gradient() {
        int[][][][] gradient = null;
        int dimX = data[0][0].length;
        int dimY = data[0].length;
        int dimZ = data.length;
        gradient = new int[dimZ - 2][dimY - 2][dimX - 2][3]; //-2 due gradient not being computable at borders
        for (int k = 1; k < dimZ - 1; k++)
            for (int j = 1; j < dimY - 1; j++)
                for (int i = 1; i < dimX - 1; i++) {
                    gradient[k - 1][j - 1][i - 1][0] = (data[k][j][i + 1] - data[k][j][i - 1]) / 2;
                    gradient[k - 1][j - 1][i - 1][1] = (data[k][j + 1][i] - data[k][j - 1][i]) / 2;
                    gradient[k - 1][j - 1][i - 1][2] = (data[k + 1][j][i] - data[k - 1][j][i]) / 2;
                }
        return gradient;
    }

//-------------------------------------------------------------------------------------------------------------------

    /**
     * This function returns an image of a contour visualisation projected along the z axis. Only for 3rd year students to complete
     * <p>
     * param gradient The gradient of the volume
     * param direction The direction of the ray along the axis
     *
     * @param isovalue The threshold value for delimitating the isosurface
     */

    public int[][] RenderContour(int[][][][] gradient, int isovalue, boolean positiveDirection) {
        int dimX = data[0][0].length;
        int dimY = data[0].length;
        int dimZ = data.length;
        int[][] projection = new int[dimY][dimX];

        for (int y = 0; y < dimY; y++) {
            for (int x = 0; x < dimX; x++) {
                double sum = 0;
                double edgeSum = 0;

                for (int z = 0; z < dimZ; z++) {
                    // Instead of directly accessing data, use trilinear interpolation
                    double interpolatedValue = trilinearInterpolation(x, y, z);
                    sum += interpolatedValue;

                    // Compute edge intensity (gradient)
                    if (z > 0 && z < dimZ - 1 && y > 0 && y < dimY - 1 && x > 0 && x < dimX - 1) {
                        double gx = gradient[z - 1][y - 1][x - 1][0];
                        double gy = gradient[z - 1][y - 1][x - 1][1];
                        edgeSum += Math.sqrt(gx * gx + gy * gy);
                    }
                }

                // Apply contrast adjustments
                double density = sum / dimZ;
                double edge = edgeSum / dimZ;
                projection[y][x] = (int) (0.7 * density + 0.3 * edge);

                // Normalize to 0-255
                projection[y][x] = Math.min(255, Math.max(0, (int) (255 * Math.log(1 + projection[y][x]) / Math.log(256))));
            }
        }
        return projection;
    }


    /**
     * This function swaps the x or y dimension with the z one, allowing projection on other faces of the volume.
     */
    void SwapZAxis(int axis) {
        if (axis == 2)
            return;
        int dimX = data[0][0].length;
        int dimY = data[0].length;
        int dimZ = data.length;
        int newvol[][][];
        if (axis == 0) {
            newvol = new int[dimX][dimY][dimZ];
            for (int k = 0; k < dimZ; k++)
                for (int j = 0; j < dimY; j++)
                    for (int i = 0; i < dimX; i++)
                        newvol[i][j][k] = data[k][j][i];
        } else {
            newvol = new int[dimY][dimZ][dimX];
            for (int k = 0; k < dimZ; k++)
                for (int j = 0; j < dimY; j++)
                    for (int i = 0; i < dimX; i++)
                        newvol[j][k][i] = data[k][j][i];
        }
        data = newvol;
    }

    //-------------------------------------------------------------------------------------------------------------------


    //	NEW METHODS

    /**
     * Trilinear interpolation for smooth sampling along the rays.
     */
    private double trilinearInterpolation(double x, double y, double z) {
        int x0 = (int) Math.floor(x);
        int x1 = x0 + 1;
        int y0 = (int) Math.floor(y);
        int y1 = y0 + 1;
        int z0 = (int) Math.floor(z);
        int z1 = z0 + 1;

        double xd = x - x0;
        double yd = y - y0;
        double zd = z - z0;

        // Ensure indices are within bounds
        x0 = Math.max(0, Math.min(x0, data[0][0].length - 1));
        x1 = Math.max(0, Math.min(x1, data[0][0].length - 1));
        y0 = Math.max(0, Math.min(y0, data[0].length - 1));
        y1 = Math.max(0, Math.min(y1, data[0].length - 1));
        z0 = Math.max(0, Math.min(z0, data.length - 1));
        z1 = Math.max(0, Math.min(z1, data.length - 1));

        // Retrieve voxel values
        double c000 = data[z0][y0][x0];
        double c100 = data[z0][y0][x1];
        double c010 = data[z0][y1][x0];
        double c110 = data[z0][y1][x1];
        double c001 = data[z1][y0][x0];
        double c101 = data[z1][y0][x1];
        double c011 = data[z1][y1][x0];
        double c111 = data[z1][y1][x1];

        // Interpolate along x-axis
        double c00 = c000 * (1 - xd) + c100 * xd;
        double c01 = c001 * (1 - xd) + c101 * xd;
        double c10 = c010 * (1 - xd) + c110 * xd;
        double c11 = c011 * (1 - xd) + c111 * xd;

        // Interpolate along y-axis
        double c0 = c00 * (1 - yd) + c10 * yd;
        double c1 = c01 * (1 - yd) + c11 * yd;

        // Interpolate along z-axis
        return c0 * (1 - zd) + c1 * zd;
    }

    /**
     * Generates a contour visualization of the 3D dataset.
     */
//    public BufferedImage generateContourImage() {
//        BufferedImage image = new BufferedImage(resolution, resolution, BufferedImage.TYPE_BYTE_GRAY);
//
//        for (int y = 0; y < resolution; y++) {
//            for (int x = 0; x < resolution; x++) {
//                double sampleX = x / zoom;
//                double sampleY = y / zoom;
//                double sampleZ = resolution / 2.0; // Middle slice
//
//                double value = trilinearInterpolation(sampleX, sampleY, sampleZ);
//                int intensity = (int) Math.min(255, Math.max(0, value * 255));
//
//                int grayscale = (intensity << 16) | (intensity << 8) | intensity;
//                image.setRGB(x, y, grayscale);
//            }
//        }
//        return image;
//    }

}

//-------------------------------------------------------------------------------------------------------------------
//-------------------------------------------------------------------------------------------------------------------

public class CW {
    /**
     * This function returns your name. Needs to be updated.
     */
    public static String Name() {
        return "I will put here my name";
    }

    /**
     * This function returns your student id. Needs to be updated.
     */
    public static int SUID() {
        return 13102000;
    }


    // ------------------------------------------------------------------------------------------------------------
//    public static void SaveImage(String name, int[][] im) {
//        BufferedImage image = new BufferedImage(im.length, im[0].length, BufferedImage.TYPE_BYTE_GRAY);
//        for (int j = 0; j < im.length; j++)
//            for (int i = 0; i < im[0].length; i++)
//                image.setRGB(j, i, im[j][i] * 256 * 256 + im[j][i] * 256 + im[j][i]);
//
//        File f = new File(name);
//        try {
//            ImageIO.write(image, "tiff", f);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public static void SaveImage(String name, int[][] im) {
        BufferedImage image = new BufferedImage(im[0].length, im.length, BufferedImage.TYPE_BYTE_GRAY);
        for (int j = 0; j < im.length; j++) {
            for (int i = 0; i < im[0].length; i++) {
                int value = Math.min(255, Math.max(0, im[j][i]));
                image.setRGB(i, j, (value << 16) | (value << 8) | value);
            }
        }
        File f = new File(name);
        try {
            ImageIO.write(image, "tiff", f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //----------------------------------------------------------------------------------------------------

    public static void SaveImageRGB(String name, int[][][] im) {
        BufferedImage image = new BufferedImage(im.length, im[0].length, BufferedImage.TYPE_INT_RGB);
        for (int j = 0; j < im.length; j++)
            for (int i = 0; i < im[0].length; i++) {
                Color c = new Color(Math.abs(im[j][i][0]), Math.abs(im[j][i][1]), Math.abs(im[j][i][2]));
                image.setRGB(j, i, c.getRGB());
            }

        File f = new File(name);
        try {
            ImageIO.write(image, "tiff", f);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //-------------------------------------------------------------------------------------------------------------------

    //The main function should not really be modified, except maybe for setting the zoom value and res values
    public static void main(String[] args) {
        System.out.println(Name());
        System.out.println(SUID());

//		our arguments
//		int width = Integer.parseInt(args[0]);
//		int height = Integer.parseInt(args[1]);
//		int depth = Integer.parseInt(args[2]);
//		int headerSize = Integer.parseInt(args[3]);
//		int isoValue = Integer.parseInt(args[4]);
//		int projectionAxis = Integer.parseInt(args[5]);
//		boolean direction = Boolean.parseBoolean(args[6]);

        // Handle optional resolution & zoom
        int resolution = (args.length > 7) ? Integer.parseInt(args[7]) : 512;
        float zoom = (args.length > 8) ? Float.parseFloat(args[8]) : 1.0f;

        //Args: width height depth header_size isovalue projection_axis direction
        //A command line example: java CW 256 256 225 62 95 0 false
        //  java CW 256 256 225 62 95 0 false 512 2.0
        Volume v = new Volume();

        v.SetResolution(resolution);
        v.SetZoom(zoom);

        v.ReadData("./bighead_den256X256X225B62H.raw", Integer.parseInt(args[0]), Integer.parseInt(args[1]), Integer.parseInt(args[2]), Integer.parseInt(args[3]));
        v.SwapZAxis(Integer.parseInt(args[5]));
        int[][][][] gradient = v.Gradient();
        int[][] im;
        im = v.RenderContour(gradient, Integer.parseInt(args[4]), Boolean.parseBoolean(args[6]));

        if (im != null) {
            SaveImage("contour.tiff", im);
            SaveImage("contour" + SUID() + ".tiff", im);
            System.out.println("Image saved as contour.tiff");
        } else {
            System.out.println("Failed to generate image");
        }
    }
}