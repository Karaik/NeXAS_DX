//package com.giga.nexasdxeditor.util;
//
//import com.giga.nexasdxeditor.service.parser.MekaParser;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.DataInputStream;
//import java.io.IOException;
//import java.io.InputStream;
//import java.nio.charset.Charset;
//
//public class ParserUtil {
//
//    public static MekaParser.Meka readMekaData(MultipartFile file, Charset encoding) throws IOException {
//        // 获取 MultipartFile 的 InputStream
//        InputStream inputStream = file.getInputStream();
//        DataInputStream br = new DataInputStream(inputStream);
//
//        MekaParser.Meka meka = new MekaParser.Meka();
//
//        // 机甲数据部分
//        br.skipBytes(4); // 跳过4个字节
//        br.readInt();  // 读取并跳过第一个int
//
//        // 读取 MekaParser 结构
//        meka.Name = getString(br, encoding);
//        meka.UnknownS = getString(br, encoding);
//        meka.UnknownI = br.readInt();
//        meka.UnknownI2 = br.readInt();
//        meka.UnknownI3 = br.readInt();
//        meka.UnknownI4 = br.readInt();
//        meka.UnknownI5 = br.readInt();
//        meka.UnknownI6 = br.readInt();
//        meka.A1 = readBytes(br, 0x18);
//        meka.UnknownI7 = br.readInt();
//        meka.UnknownI8 = br.readInt();
//        meka.UnknownI9 = br.readInt();
//        meka.UnknownI10 = br.readInt();
//        meka.A2 = readBytes(br, 0x44);
//        meka.UnknownI11 = br.readInt();
//        meka.UnknownI12 = br.readInt();
//        meka.UnknownI13 = br.readInt();
//        meka.UnknownI14 = br.readInt();
//        meka.UnknownI15 = br.readInt();
//        meka.UnknownI16 = br.readInt();
//        meka.UnknownI17 = br.readInt();
//        meka.UnknownI18 = br.readInt();
//        meka.UnknownI19 = br.readInt();
//        meka.UnknownI20 = br.readInt();
//        meka.UnknownI21 = br.readInt();
//        meka.UnknownI22 = br.readInt();
//        meka.UnknownI23 = br.readInt();
//        meka.UnknownI24 = br.readInt();
//        meka.UnknownI25 = br.readInt();
//        meka.UnknownI26 = br.readInt();
//        meka.A3 = readBytes(br, 0x18);
//
//        // 跳转到 4 的位置，读取下一个数据段
//        br.skipBytes(4);
//        br.readInt();
//        meka.UnknownStruct = new MekaParser.MekaUnknown[34];
//        for (int i = 0; i < 34; i++) {
//            MekaParser.MekaUnknown u = new MekaParser.MekaUnknown();
//            u.U1 = br.readInt();
//            u.U2 = br.readInt();
//            meka.UnknownStruct[i] = u;
//        }
//
//        int count2 = br.readInt();
//        meka.UnknownStruct2 = new MekaParser.MekaUnknown2[count2];
//        for (int i = 0; i < count2; i++) {
//            MekaParser.MekaUnknown2 u = new MekaParser.MekaUnknown2();
//            u.US1 = getString(br, encoding);
//            u.US2 = getString(br, encoding);
//            u.UI1 = br.readInt();
//            u.UI2 = br.readInt();
//            u.UI3 = br.readInt();
//            meka.UnknownStruct2[i] = u;
//        }
//
//        br.skipBytes(4);  // 跳过一些字节
//        br.readInt(); // 再次读取并跳过
//
//        // 读取武器数据
//        count2 = br.readInt();
//        meka.Weapons = new MekaParser.MekaWeapon[count2];
//        for (int i = 0; i < count2; i++) {
//            MekaParser.MekaWeapon w = new MekaParser.MekaWeapon();
//            int a = br.readInt();
//            if (a > 0) {
//                w.Name = getString(br, encoding);
//                w.US1 = getString(br, encoding);
//                w.US2 = getString(br, encoding);
//                w.UI1 = br.readInt();
//                w.UI2 = br.readInt();
//                w.UI3 = br.readInt();
//                w.UI4 = br.readInt();
//                w.UI5 = br.readInt();
//                w.UI6 = br.readInt();
//                w.UI7 = br.readInt();
//                w.UI8 = br.readInt();
//                w.UI9 = br.readInt();
//                w.UI10 = br.readInt();
//                w.UI11 = br.readInt();
//                w.UI12 = br.readInt();
//                w.UI13 = br.readInt();
//                w.UI14 = br.readInt();
//                w.UI15 = br.readInt();
//                w.UI16 = br.readInt();
//                w.UI17 = br.readInt();
//                w.UI18 = br.readInt();
//                w.UI19 = br.readInt();
//                w.UI20 = br.readInt();
//                w.UI21 = br.readInt();
//            }
//            meka.Weapons[i] = w;
//        }
//
//        return meka;
//    }
//
//    public static String getString(DataInputStream br, Charset encoding) throws IOException {
//        long pos = br.available();
//        while (br.readByte() != 0) {
//        }
//        int len = (int) (br.available() - pos);
//        br.reset();
//        byte[] bytes = new byte[len];
//        br.readFully(bytes);
//        return new String(bytes, encoding).replaceAll("\0+$", "");
//    }
//
//    private static byte[] readBytes(DataInputStream br, int length) throws IOException {
//        byte[] bytes = new byte[length];
//        br.readFully(bytes);
//        return bytes;
//    }
//
//
//}
