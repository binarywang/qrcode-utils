package com.github.binarywang.utils.qrcode;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.Binarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.NotFoundException;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * <pre>
 * Created by Binary Wang on 2017-01-05.
 * @author <a href="https://github.com/binarywang">binarywang(Binary Wang)</a>
 * </pre>
 */
public class QrcodeUtils {
  private static final int DEFAULT_LENGTH = 400;// 生成二维码的默认边长，因为是正方形的，所以高度和宽度一致
  private static final String FORMAT = "jpg";// 生成二维码的格式

  private static Logger logger = LoggerFactory.getLogger(QrcodeUtils.class);

  /**
   * 根据内容生成二维码数据
   *
   * @param content 二维码文字内容[为了信息安全性，一般都要先进行数据加密]
   * @param length  二维码图片宽度和高度
   */
  public static BitMatrix createQrcodeMatrix(String content, int length) throws Exception {
    return createQrcodeMatrix(content, length, ErrorCorrectionLevel.H);
  }
  
  public static BitMatrix createQrcodeMatrix(String content, int length, ErrorCorrectionLevel level) throws Exception {
    Map<EncodeHintType, Object> hints = Maps.newEnumMap(EncodeHintType.class);
    // 设置字符编码
    hints.put(EncodeHintType.CHARACTER_SET, Charsets.UTF_8.name());
    // 指定纠错等级
    hints.put(EncodeHintType.ERROR_CORRECTION, level);
    
    try {
      return new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, length, length, hints);
    } catch (WriterException e) {
      throw new RuntimeException("内容为：【" + content + "】的二维码生成失败！", e);
    }
  }

  /**
   * 根据指定边长创建生成的二维码，允许配置logo属性
   *
   * @param content  二维码内容
   * @param length   二维码的高度和宽度
   * @param logoFile logo 文件对象，可以为空
   * @param logoConfig logo配置，可设置logo展示长宽，边框颜色
   * @return 二维码图片的字节数组
   */
  public static byte[] createQrcode(String content, int length, File logoFile, MatrixToLogoImageConfig logoConfig) throws Exception {
    if (logoFile != null && !logoFile.exists()) {
      throw new IllegalArgumentException("请提供正确的logo文件！");
    }
    InputStream logo = new FileInputStream(logoFile);
    return createQrcode(content, length, logo, logoConfig);
  }
  
  public static byte[] createQrcode(String content, int length, InputStream logo, MatrixToLogoImageConfig logoConfig) throws Exception {
    BufferedImage img =  generateQRCodeImage(content, length, logo, logoConfig);
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ImageIO.write(img, FORMAT, baos);
    return baos.toByteArray();
  }

  /**
   * 根据指定边长创建生成的二维码
   *
   * @param content  二维码内容
   * @param length   二维码的高度和宽度
   * @param logoFile logo 文件对象，可以为空
   * @return 二维码图片的字节数组
   */
  public static byte[] createQrcode(String content, int length, File logoFile) throws Exception {
    return  createQrcode(content, length, logoFile, new MatrixToLogoImageConfig());
  }

  /**
   * 创建生成默认高度(400)的二维码图片
   * 可以指定是否贷logo
   *
   * @param content  二维码内容
   * @param logoFile logo 文件对象，可以为空
   * @return 二维码图片的字节数组
   */
  public static byte[] createQrcode(String content, File logoFile) throws Exception {
    return createQrcode(content, DEFAULT_LENGTH, logoFile);
  }
  
  public static BufferedImage generateQRCodeImage(String content, int length, InputStream logo, MatrixToLogoImageConfig logoConfig) throws Exception {
    // 生成二维码图像
    BitMatrix qrCodeMatrix = createQrcodeMatrix(content, length);
    BufferedImage img =  MatrixToImageWriter.toBufferedImage(qrCodeMatrix);
    try {
      if (logo != null) {
        overlapImage(img, FORMAT, logo, logoConfig);
      }
    } catch (Exception e) {
      throw new RuntimeException("为二维码添加LOGO时失败！", e);
    }
    return img;
  }
  
  public static BufferedImage generateQRCodeImage(String content, int length, File logoFile, MatrixToLogoImageConfig logoConfig) throws Exception {
    if (logoFile != null && !logoFile.exists()) {
      throw new IllegalArgumentException("请提供正确的logo文件！");
    }
    InputStream logo = new FileInputStream(logoFile);
    return generateQRCodeImage(content, length, logo, logoConfig);
  }
  
  public static BufferedImage generateQRCodeImage(String content, int length, InputStream logo) throws Exception {
    return generateQRCodeImage(content, length, logo, new MatrixToLogoImageConfig());
  }
  
  public static BufferedImage generateQRCodeImage(String content, int length, File logoFile) throws Exception {
    return generateQRCodeImage(content, length, logoFile, new MatrixToLogoImageConfig());
  }
  
  public static BufferedImage generateQRCodeImage(String content, InputStream logo) throws Exception {
    return generateQRCodeImage(content, DEFAULT_LENGTH, logo);
  }
  
  public static BufferedImage generateQRCodeImage(String content, File logoFile) throws Exception {
    return generateQRCodeImage(content, DEFAULT_LENGTH, logoFile);
  }

  /**
   * 将logo添加到二维码中间
   *
   * @param image     生成的二维码图片对象
   * @param imagePath 图片保存路径
   * @param logoFile  logo文件对象
   * @param format    图片格式
   */
  private static void overlapImage(final BufferedImage image, String format, final InputStream logo,
      MatrixToLogoImageConfig logoConfig) throws IOException {
    BufferedImage logoImg = ImageIO.read(logo);
    Graphics2D g = image.createGraphics();
    // 考虑到logo图片贴到二维码中，建议大小不要超过二维码的1/5;
    int width = image.getWidth() / logoConfig.getLogoPart();
    int height = image.getHeight() / logoConfig.getLogoPart();
    // logo起始位置，此目的是为logo居中显示
    int x = (image.getWidth() - width) / 2;
    int y = (image.getHeight() - height) / 2;
    // 绘制图
    g.drawImage(logoImg, x, y, width, height, null);

    // 给logo画边框
    // 构造一个具有指定线条宽度以及 cap 和 join 风格的默认值的实心 BasicStroke
    g.setStroke(new BasicStroke(logoConfig.getBorder()));
    g.setColor(logoConfig.getBorderColor());
    g.drawRect(x, y, width, height);

    g.dispose();
  }

  /**
   * 解析二维码
   *
   * @param file 二维码文件内容
   * @return 二维码的内容
   */
  public static String decodeQrcode(File file) throws IOException, NotFoundException {
    BufferedImage image = ImageIO.read(file);
    LuminanceSource source = new BufferedImageLuminanceSource(image);
    Binarizer binarizer = new HybridBinarizer(source);
    BinaryBitmap binaryBitmap = new BinaryBitmap(binarizer);
    Map<DecodeHintType, Object> hints = Maps.newEnumMap(DecodeHintType.class);
    hints.put(DecodeHintType.CHARACTER_SET, Charsets.UTF_8.name());
    return new MultiFormatReader().decode(binaryBitmap, hints).getText();
  }
}
