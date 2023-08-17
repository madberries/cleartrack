package pac.example;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BmpImageDecoder {
  public static final int FILE_HEADER_SIZE = 12;
  public static final int BITMAP_INFO_SIZE = 40;
  public static final int WIDTH_MAXIMUM = 4000;
  public static final int HEIGHT_MAXIMUM = 4000;

  InputStream inputStream;
  OutputStream outputStream;

  BmpFileHeader fileHeader;
  BITMAPINFOHEADER bitmapInfoHeader;
  BufferedImage image;

  int bytesPerRow = 0;
  int paddingPerRow = 0;
  int bytesPerPixel = 1;
  int bitsPerPixel = 8;
  int pixelsPerByte = 1;
  int pixelShift = 0;
  int pixelMask = 0xFF;
  int colorTableSize = 0;

  byte[] intBuffer = new byte[4];

  public BufferedImage read(File file) throws FileNotFoundException, IOException {
    FileInputStream fis = null;
    try {
      fis = new FileInputStream(file);
      int gap_sz, fsize = fis.available();
      inputStream = fis;
      readFileHeaders(fsize);
      gap_sz = verifyFileHeaders(fileHeader, bitmapInfoHeader, fsize);
      verifySupportedFormat(bitmapInfoHeader);
      if (gap_sz > 0) {
        long actualSkip = inputStream.skip(gap_sz);
        if (actualSkip != gap_sz) {
          System.out.println("Warning: didn't skip requested amount: " + actualSkip + " " + gap_sz);
        }
      }
      image = readRgbPixelArray();
      return image;
    } finally {
      if (fis != null)
        fis.close();
    }
  }

  public void readFileHeaders(int fsize) throws IOException {
    byte b0, b1;

    if (fsize < FILE_HEADER_SIZE + BITMAP_INFO_SIZE + 2)
      throwFileFormatException("File header missing");

    b0 = readByte();
    b1 = readByte();
    if (b0 != 0x42 || b1 != 0x4D)
      throwFileFormatException("Magic 'BM' not found");

    fileHeader = readFileHeader();
    if (fileHeader.file_sz < 0 || fileHeader.file_sz > fsize)
      throwFileFormatException("File size mismatch");

    bitmapInfoHeader = readBitmapInfoHeader();
    if (bitmapInfoHeader.header_sz > BITMAP_INFO_SIZE) {
      long gap_sz = bitmapInfoHeader.header_sz - BITMAP_INFO_SIZE;
      long actualSkip = inputStream.skip(gap_sz);
      if (actualSkip != gap_sz) {
        System.out.println("Warning: didn't skip requested amount: " + actualSkip + " " + gap_sz);
      }
    }
  }

  public int verifyFileHeaders(BmpFileHeader bfh, BITMAPINFOHEADER bih, int fsize) {
    int bmap_size, hdr_size = 2 + FILE_HEADER_SIZE + bih.header_sz;

    bitsPerPixel = bih.bitspp;

    if (bitsPerPixel <= 8) {
      if (bitsPerPixel != 1 && bitsPerPixel != 2 && bitsPerPixel != 4 && bitsPerPixel != 8)
        throwFileFormatException("invalid bits per pixel");

      pixelsPerByte = 8 / bitsPerPixel;
      colorTableSize = (1 << bitsPerPixel) * 4;
      if (bih.ncolors != 0) {
        if (bih.ncolors * 4 > colorTableSize)
          throwFileFormatException("invalid number of colors");
        colorTableSize = bih.ncolors * 4;
      }
      if (bitsPerPixel < 8) {
        pixelShift = bitsPerPixel;
        pixelMask = (1 << bitsPerPixel) - 1;
      }
    } else {
      if (bitsPerPixel != 16 && bitsPerPixel != 24 && bitsPerPixel != 32)
        throwFileFormatException("invalid bits per pixel");
      bytesPerPixel = bih.bitspp / 8;
      if (bih.ncolors != 0)
        throwFileFormatException("invalid number of colors");
    }

    if (bih.nimpcolors != 0 && bih.nimpcolors * 4 > colorTableSize)
      throwFileFormatException("invalid number of important colors");

    if (bih.width > WIDTH_MAXIMUM || bih.height > HEIGHT_MAXIMUM)
      throwFileFormatException("invalid width or height");

    bytesPerRow = (bih.width * bytesPerPixel) / pixelsPerByte;
    paddingPerRow = bytesPerRow % 4;
    if (paddingPerRow > 0) {
      paddingPerRow = 4 - paddingPerRow;
      bytesPerRow += paddingPerRow;
    }
    bmap_size = bih.height * bytesPerRow;
    if (bih.bmp_bytesz != 0 && bih.bmp_bytesz != bmap_size)
      throwFileFormatException("invalid bitmap size");

    if (bfh.file_sz > fsize)
      throwFileFormatException("File size in header invalid");
    fsize = bfh.file_sz;

    if (bfh.pixelArrayOffset < hdr_size + colorTableSize)
      throwFileFormatException("Pixel data offset invalid");

    fsize -= hdr_size + colorTableSize;
    if (fsize < bmap_size)
      throwFileFormatException("File size too small for bitmap");

    return bfh.pixelArrayOffset - colorTableSize - hdr_size;
  }

  public void verifySupportedFormat(BITMAPINFOHEADER bih) {
    if (bih.compress_type != 0)
      throwUnsupportFormatException("compressed pixel data");
    if (bih.bitspp != 24)
      throwUnsupportFormatException("must be 24 bits per pixel");
  }

  public BufferedImage readRgbPixelArray() throws IOException {
    ColorModel cm;

    WritableRaster raster;
    BufferedImage image;

    int w, h, rgb, pad = 0;
    int[] colors;
    w = bitmapInfoHeader.width;
    h = bitmapInfoHeader.height;
    pad = (w * 3) % 4;
    if (pad > 0)
      pad = 4 - pad;
    cm = ColorModel.getRGBdefault();
    colors = new int[cm.getNumComponents()];

    raster = cm.createCompatibleWritableRaster(w, h);
    image = new BufferedImage(cm, raster, true, null);

    for (int y = h - 1; y > 0; y--) {
      for (int x = 0; x < w; x++) {
        rgb = readRGB();
        colors[0] = cm.getRed(rgb);
        colors[1] = cm.getGreen(rgb);
        colors[2] = cm.getBlue(rgb);
        if (colors.length >= 4)
          colors[3] = 0xFF;
        raster.setPixel(x, y, colors);
      }
      if (pad > 0)
        readInt(pad);
    }

    return image;
  }

  public void write(RenderedImage bi, String fname) throws FileNotFoundException, IOException {
    write(bi, new File(fname));
  }

  public void write(RenderedImage bi, File file) throws FileNotFoundException, IOException {
    BmpFileHeader bfh;
    BITMAPINFOHEADER bih;

    if (bitmapInfoHeader == null) {
      bfh = new BmpFileHeader();
      bfh.applParam1 = 0;
      bfh.applParam2 = 0;
      bih = new BITMAPINFOHEADER();
      bih.hres = 4724;
      bih.vres = 4724;
    }

    else {
      bfh = fileHeader.clone();
      bih = bitmapInfoHeader.clone();
    }

    write(bi, file, bfh, bih);
  }

  public void write(RenderedImage bi, File file, BmpFileHeader bfh, BITMAPINFOHEADER bih)
      throws FileNotFoundException, IOException {

    ColorModel cm;
    int bpr, bmsz, hsz = FILE_HEADER_SIZE + BITMAP_INFO_SIZE + 2;

    bih.header_sz = BITMAP_INFO_SIZE;
    bih.width = bi.getWidth();
    bih.height = bi.getHeight();
    bih.nplanes = 1;
    bih.bitspp = 8;
    bih.ncolors = 0;
    bih.nimpcolors = 0;
    bih.compress_type = 0;

    bfh.pixelArrayOffset = hsz + 1024;
    bpr = bih.width % 4;
    if (bpr > 0)
      bpr = 4 - bpr;
    bpr += bih.width;
    bmsz = bpr * bih.height;
    bih.bmp_bytesz = 0;
    bfh.file_sz = bfh.pixelArrayOffset + bmsz;

    FileOutputStream fos = new FileOutputStream(file);
    outputStream = fos;
    writeFileHeader(bfh, bih);
    cm = bi.getColorModel();
    if (cm instanceof IndexColorModel)
      writeColorTable((IndexColorModel) cm);
    writePixelArray(bi);
    fos.close();
  }

  public void writeColorTable(IndexColorModel cm) throws IOException {
    byte[] reds, greens, blues;
    int cm_size = cm.getMapSize();
    reds = new byte[cm_size];
    greens = new byte[cm_size];
    blues = new byte[cm_size];
    cm.getReds(reds);
    cm.getGreens(greens);
    cm.getBlues(blues);

    for (int i = 0; i < cm_size; i++) {
      writeByte(blues[i]);
      writeByte(greens[i]);
      writeByte(reds[i]);
      writeByte((byte) 0);
    }
  }

  public void writePixelArray(RenderedImage image) throws IOException {
    ColorModel cm;
    Raster raster;
    int w, h, pixel, ncomp, pad = 0;
    int[] colors;

    w = image.getWidth();
    h = image.getHeight();
    pad = w % 4;
    if (pad > 0)
      pad = 4 - pad;

    raster = image.getData();
    cm = image.getColorModel();
    ncomp = cm.getNumComponents();
    colors = new int[ncomp];

    for (int y = h - 1; y >= 0; y--) {
      for (int x = 0; x < w; x++) {
        pixel = raster.getSample(x, y, 0);
        cm.getComponents(pixel, colors, 0);
        writeByte((byte) (pixel & 0xFF));
      }
      if (pad > 0)
        writeInt(pad, 0);
    }
  }

  public void writeFileHeader(BmpFileHeader bfh, BITMAPINFOHEADER bih) throws IOException {

    writeByte((byte) 0x42);
    writeByte((byte) 0x4D);

    writeFileHeader(bfh);
    writeBitmapInfoHeader(bih);
  }

  public BmpFileHeader readFileHeader() throws IOException {
    BmpFileHeader bfh = new BmpFileHeader();
    bfh.file_sz = readInt();
    bfh.applParam1 = readShort();
    bfh.applParam1 = readShort();
    bfh.pixelArrayOffset = readInt();
    if (bfh.file_sz <= 0 || bfh.pixelArrayOffset <= 0)
      throwFileFormatException("Bad file header");
    return bfh;
  }

  public void writeFileHeader(BmpFileHeader bfh) throws IOException {
    writeInt(bfh.file_sz);
    writeShort(bfh.applParam1);
    writeShort(bfh.applParam1);
    writeInt(bfh.pixelArrayOffset);
  }

  public BITMAPINFOHEADER readBitmapInfoHeader() throws IOException {
    BITMAPINFOHEADER bih = new BITMAPINFOHEADER();

    bih.header_sz = readInt();

    if (bih.header_sz < 40) {
      if (bih.header_sz == 12)
        throwUnsupportFormatException("Header BITMAPCOREINFO");
      throwFileFormatException("BitmapInfoHeader size too small");
    }

    if (bih.header_sz > 40) {

      if (bih.header_sz != 108 && bih.header_sz != 108)
        throwUnsupportFormatException("Unknown BitmapInfoHeader size");
    }

    bih.width = readInt();
    bih.height = readInt();
    bih.nplanes = readShort();
    bih.bitspp = readShort();
    bih.compress_type = readInt();
    bih.bmp_bytesz = readInt();
    bih.hres = readInt();
    bih.vres = readInt();
    bih.ncolors = readInt();
    bih.nimpcolors = readInt();

    if (bih.nplanes != 1 || bih.width <= 0 || bih.height <= 0 || bih.bitspp <= 0)
      throwFileFormatException("Bad bitmap header");
    if (bih.bmp_bytesz < 0 || bih.ncolors < 0 || bih.nimpcolors < 0)
      throwFileFormatException("Bad bitmap header");

    return bih;
  }

  public void writeBitmapInfoHeader(BITMAPINFOHEADER bih) throws IOException {
    writeInt(bih.header_sz);
    writeInt(bih.width);
    writeInt(bih.height);
    writeShort(bih.nplanes);
    writeShort(bih.bitspp);
    writeInt(bih.compress_type);
    writeInt(bih.bmp_bytesz);
    writeInt(bih.hres);
    writeInt(bih.vres);
    writeInt(bih.ncolors);
    writeInt(bih.nimpcolors);
  }

  public int readInt() throws IOException {
    return readInt(4);
  }

  public int readRGB() throws IOException {
    return readInt(3);
  }

  public int readInt(int bcnt) throws IOException {
    int rcnt = 0;
    int rval = 0;

    if ((rcnt = inputStream.read(intBuffer, 0, bcnt)) < 0 || rcnt != bcnt)
      throwEndOfDataException();

    for (int i = bcnt - 1; i >= 0; i--) {
      rval <<= 8;
      rval |= intBuffer[i] & 0xFF;
    }

    return rval;
  }

  public short readShort() throws IOException {
    int b0, b1;

    b0 = inputStream.read();
    if (b0 < 0)
      throwEndOfDataException();

    b1 = inputStream.read();
    if (b1 < 0)
      throwEndOfDataException();

    return (short) (((b1 << 8) & 0xFF00) | (b0 & 0xFF));
  }

  public byte readByte() throws IOException {
    int b0;

    b0 = inputStream.read();
    if (b0 < 0)
      throwEndOfDataException();

    return (byte) b0;
  }

  public void writeInt(int rval) throws IOException {
    writeInt(4, rval);
  }

  public void writeInt(int bcnt, int rval) throws IOException {
    for (int i = 0; i < bcnt; i++) {
      intBuffer[i] = (byte) (rval & 0xFF);
      rval >>= 8;
    }
    outputStream.write(intBuffer, 0, bcnt);
  }

  public void writeShort(short rval) throws IOException {
    byte b0, b1;
    b0 = (byte) (rval & 0xFF);
    b1 = (byte) ((rval >> 8) & 0xFF);
    outputStream.write(b0);
    outputStream.write(b1);
  }

  public void writeByte(byte rval) throws IOException {
    outputStream.write(rval);
  }

  public void throwEndOfDataException() {
    throw new IllegalArgumentException("BMP file format error: Insufficient Data");
  }

  public void throwFileFormatException(String msg) {
    throw new IllegalArgumentException("BMP file format error: " + msg);
  }

  public void throwUnsupportFormatException(String msg) {
    throw new UnsupportedOperationException("BMP file format unsupported: " + msg);
  }

  public class BmpFileHeader {
    public int file_sz;
    public short applParam1;
    public short applParam2;
    public int pixelArrayOffset;

    public BmpFileHeader clone() {
      BmpFileHeader bfh = new BmpFileHeader();
      bfh.file_sz = file_sz;
      bfh.applParam1 = applParam1;
      bfh.applParam1 = applParam1;
      bfh.pixelArrayOffset = pixelArrayOffset;
      return bfh;
    }
  }

  public class BITMAPINFOHEADER {
    public int header_sz;
    public int width;
    public int height;
    public short nplanes;
    public short bitspp;
    public int compress_type;
    public int bmp_bytesz;
    public int hres;
    public int vres;
    public int ncolors;
    public int nimpcolors;

    public BITMAPINFOHEADER clone() {
      BITMAPINFOHEADER bih = new BITMAPINFOHEADER();
      bih.header_sz = header_sz;
      bih.width = width;
      bih.height = height;
      bih.nplanes = nplanes;
      bih.bitspp = bitspp;
      bih.bmp_bytesz = bmp_bytesz;
      bih.hres = hres;
      bih.vres = vres;
      bih.ncolors = ncolors;
      bih.nimpcolors = nimpcolors;
      return bih;
    }
  }
}
