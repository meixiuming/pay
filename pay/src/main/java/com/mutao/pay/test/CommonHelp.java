package com.mutao.pay.test;

import org.bouncycastle.crypto.digests.SM3Digest;
import org.bouncycastle.pqc.math.linearalgebra.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cglib.beans.BeanMap;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 工具类
 *
 * @author baojing
 *
 */
public class CommonHelp {

	private CommonHelp() {

	}

	private final static Logger log = LoggerFactory.getLogger(CommonHelp.class);

	private static BigDecimal onehundred = new BigDecimal(100);

	private static DecimalFormat formt = new DecimalFormat("0");

	/**
	 * 返回 yyyy-MM-dd hh:mm:ss 格式的当前时间
	 *
	 * @param date
	 * @return
	 */
	public static String formate(Date date) {
		SimpleDateFormat dateforamte = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		return dateforamte.format(date);
	}

	/**
	 * 从左向右补充字符
	 *
	 * @param src
	 * @param len
	 * @param ch
	 * @return
	 */
	public static String padLeft(String src, int len, char ch) {
		int diff = len - src.length();
		if (diff <= 0) {
			return src;
		}

		char[] charr = new char[len];
		System.arraycopy(src.toCharArray(), 0, charr, 0, src.length());
		for (int i = src.length(); i < len; i++) {
			charr[i] = ch;
		}
		return new String(charr);
	}

	/**
	 * 从右向左补充字符
	 *
	 * @param src
	 * @param len
	 * @param ch
	 * @return
	 */
	public static String padRight(String src, int len, char ch) {
		int diff = len - src.length();
		if (diff <= 0) {
			return src;
		}

		char[] charr = new char[len];
		System.arraycopy(src.toCharArray(), 0, charr, diff, src.length());
		for (int i = 0; i < diff; i++) {
			charr[i] = ch;
		}
		return new String(charr);
	}

	/**
	 * 元单位金额格式化为 12位分单位字符串
	 *
	 * @param amt
	 * @return
	 */
	public static String amtformat(BigDecimal amt) {
		if (amt != null) {
			String value = formt.format(onehundred.multiply(amt));
			return CommonHelp.padRight(value, 12, '0');
		} else {
			return CommonHelp.padRight("0", 12, '0');
		}
	}

	/**
	 * 12位分单位字符串转为元单位金额
	 *
	 * @param amt
	 * @return
	 */
	public static String amtDeFormat(String amt) {
		try {
			BigDecimal dec = new BigDecimal(amt);
			DecimalFormat formt = new DecimalFormat("0.00");
			dec = dec.divide(onehundred);
			return formt.format(dec);
		} catch (Exception e) {
			return "";
		}
	}

	/**
	 * 获取一个16位的随机字符串
	 *
	 * @return
	 */
	public static String createNonceStr(int length) {
		char[] str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
		char[] chars = new char[length];
		Random rand = new Random();
		for (int i = 0; i < length; i++) {
			char charV = str[rand.nextInt(str.length)];
			chars[i] = charV;
		}
		return new String(chars);
	}

	/**
	 * 获取一个随机的数字或者字母
	 *
	 * @return
	 */
	public static String getRandomStr() { // NO_UCD (unused code)
		char[] str = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
		Random rand = new Random();
		return str[rand.nextInt(str.length)] + "";
	}


	/**
	 * 解密报文
	 *
	 * @param input
	 * @param key
	 * @return
	 * @throws Exception
	 */
	private static byte[] decrypt3DES(byte[] input, byte[] key) throws Exception {
		Cipher c = Cipher.getInstance("DESede/ECB/PKCS5Padding");
		c.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "DESede"));
		return c.doFinal(input);
	}

	/**
	 * 十进制转字节
	 *
	 * @param hex
	 * @return
	 */
	public static byte[] hexToBytes(String hex) {
		hex = hex.length() % 2 != 0 ? "0" + hex : hex;

		byte[] b = new byte[hex.length() / 2];
		for (int i = 0; i < b.length; i++) {
			int index = i * 2;
			int v = Integer.parseInt(hex.substring(index, index + 2), 16);
			b[i] = (byte) v;
		}
		return b;
	}

	/**
	 * 字节转十进制
	 *
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		String hexArray = "0123456789abcdef";
		StringBuilder sb = new StringBuilder(bytes.length * 2);
		for (byte b : bytes) {
			int bi = b & 0xff;
			sb.append(hexArray.charAt(bi >> 4));
			sb.append(hexArray.charAt(bi & 0xf));
		}
		return sb.toString();
	}

	/**
	 * sha256 字符串
	 *
	 * @param data
	 * @return
	 */
	public static String sha256(String data) {
		return sha256(data.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * sha256 字节数组
	 *
	 * @param data
	 * @return
	 */
	private static String sha256(byte[] data) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			return bytesToHex(md.digest(data));
		} catch (Exception ex) {
			return null;
		}
	}

	public static String encrypt(String paramStr) {
		// 将字符串转换成byte数组
		byte[] srcData = paramStr.getBytes(StandardCharsets.UTF_8);
		// 调用hash()
		byte[] resultHash = hash(srcData);
		// 将返回的hash值转换成16进制字符串
		return ByteUtils.toHexString(resultHash);
	}

	public static byte[] hash(byte[] srcData) {
		SM3Digest digest = new SM3Digest();
		digest.update(srcData, 0, srcData.length);
		byte[] hash = new byte[digest.getDigestSize()];
		digest.doFinal(hash, 0);
		return hash;
	}

	public static boolean verify(String srcStr, String sm3HexString) {
		byte[] srcData = srcStr.getBytes(StandardCharsets.UTF_8);
		byte[] sm3Hash = ByteUtils.fromHexString(sm3HexString);
		byte[] newHash = hash(srcData);
		return Arrays.equals(newHash, sm3Hash);
	}


	// 校验手机号
	public static boolean isPhoneNumber(String phoneNumber) {
		String regex = "^((13[0-9])|(14[5,7,9])|(15([0-3]|[5-9]))|(166)|(17[0,1,3,5,6,7,8])|(18[0-9])|(19[8|9]))\\d{8}$";
		phoneNumber = phoneNumber.trim();
		if (phoneNumber.length() != 11) {
			return false;
		} else {
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(phoneNumber);
			return m.matches();
		}
	}

	public static boolean isIDNumber(String IDNumber) {
		if (IDNumber == null || "".equals(IDNumber)) {
			return false;
		}
		// 定义判别用户身份证号的正则表达式（15位或者18位，最后一位可以为字母）
		String regularExpression = "(^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$)|"
				+ "(^[1-9]\\d{5}\\d{2}((0[1-9])|(10|11|12))(([0-2][1-9])|10|20|30|31)\\d{3}$)";
		// 假设18位身份证号码:41000119910101123X 410001 19910101 123X
		// ^开头
		// [1-9] 第一位1-9中的一个 4
		// \\d{5} 五位数字 10001（前六位省市县地区）
		// (18|19|20) 19（现阶段可能取值范围18xx-20xx年）
		// \\d{2} 91（年份）
		// ((0[1-9])|(10|11|12)) 01（月份）
		// (([0-2][1-9])|10|20|30|31)01（日期）
		// \\d{3} 三位数字 123（第十七位奇数代表男，偶数代表女）
		// [0-9Xx] 0123456789Xx其中的一个 X（第十八位为校验值）
		// $结尾

		// 假设15位身份证号码:410001910101123 410001 910101 123
		// ^开头
		// [1-9] 第一位1-9中的一个 4
		// \\d{5} 五位数字 10001（前六位省市县地区）
		// \\d{2} 91（年份）
		// ((0[1-9])|(10|11|12)) 01（月份）
		// (([0-2][1-9])|10|20|30|31)01（日期）
		// \\d{3} 三位数字 123（第十五位奇数代表男，偶数代表女），15位身份证不含X
		// $结尾

		boolean matches = IDNumber.matches(regularExpression);

		// 判断第18位校验值
		if (matches) {

			if (IDNumber.length() == 18) {
				try {
					char[] charArray = IDNumber.toCharArray();
					// 前十七位加权因子
					int[] idCardWi = { 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2 };
					// 这是除以11后，可能产生的11位余数对应的验证码
					String[] idCardY = { "1", "0", "X", "9", "8", "7", "6", "5", "4", "3", "2" };
					int sum = 0;
					for (int i = 0; i < idCardWi.length; i++) {
						int current = Integer.parseInt(String.valueOf(charArray[i]));
						int count = current * idCardWi[i];
						sum += count;
					}
					char idCardLast = charArray[17];
					int idCardMod = sum % 11;
					if (idCardY[idCardMod].toUpperCase().equals(String.valueOf(idCardLast).toUpperCase())) {
						return true;
					} else {
						log.error("身份证最后一位:" + String.valueOf(idCardLast).toUpperCase() + "错误,正确的应该是:"
								+ idCardY[idCardMod].toUpperCase());
						return false;
					}

				} catch (Exception e) {
					log.error(e.getMessage());
					return false;
				}
			}

		}
		return matches;
	}


	/**
	 * 将字符串数组转换为字符串
	 *
	 * @param strArr
	 * @return
	 */
	public static String convertArrayToString(String[] strArr) {
		if (strArr == null || strArr.length == 0) {
			return "";
		}
		String res = "";
		for (int i = 0, len = strArr.length; i < len; i++) {
			res += strArr[i];
			if (i < len - 1) {
				res += ",";
			}
		}
		return res;
	}

	/**
	 * 将对象装换为map
	 *
	 * @param bean
	 * @return
	 */
	public static <T> Map<String, String> beanToMap(T bean) {
		Map<String, String> map = new HashMap<String, String>();
		if (bean != null) {
			BeanMap beanMap = BeanMap.create(bean);
			for (Object key : beanMap.keySet()) {
				if(beanMap.get(key) != null){
					map.put(key + "", beanMap.get(key).toString());
				}
			}
		}
		return map;
	}

	/**
	 * 将map装换为javabean对象
	 *
	 * @param map
	 * @param bean
	 * @return
	 */
	public static <T> T mapToBean(Map<String, Object> map, T bean) {
		BeanMap beanMap = BeanMap.create(bean);
		beanMap.putAll(map);
		return bean;
	}

	/**
	 * 解析应答字符串，生成应答要素
	 *
	 * @param str
	 *            需要解析的字符串
	 * @return 解析的结果map
	 * @throws UnsupportedEncodingException
	 */
	public static Map<String, String> parseQString(String str) throws UnsupportedEncodingException {

		Map<String, String> map = new HashMap<String, String>();
		int len = str.length();
		StringBuilder temp = new StringBuilder();
		char curChar;
		String key = null;
		boolean isKey = true;
		boolean isOpen = false;// 值里有嵌套
		char openName = 0;
		if (len > 0) {
			for (int i = 0; i < len; i++) {// 遍历整个带解析的字符串
				curChar = str.charAt(i);// 取当前字符
				if (isKey) {// 如果当前生成的是key

					if (curChar == '=') {// 如果读取到=分隔符
						key = temp.toString();
						temp.setLength(0);
						isKey = false;
					} else {
						temp.append(curChar);
					}
				} else {// 如果当前生成的是value
					if (isOpen) {
						if (curChar == openName) {
							isOpen = false;
						}

					} else {// 如果没开启嵌套
						if (curChar == '{') {// 如果碰到，就开启嵌套
							isOpen = true;
							openName = '}';
						}
						if (curChar == '[') {
							isOpen = true;
							openName = ']';
						}
					}
					if (curChar == '&' && !isOpen) {// 如果读取到&分割符,同时这个分割符不是值域，这时将map里添加
						putKeyValueToMap(temp, isKey, key, map);
						temp.setLength(0);
						isKey = true;
					} else {
						temp.append(curChar);
					}
				}

			}
			putKeyValueToMap(temp, isKey, key, map);
		}
		return map;
	}

	private static void putKeyValueToMap(StringBuilder temp, boolean isKey, String key, Map<String, String> map) throws UnsupportedEncodingException {
		if (isKey) {
			key = temp.toString();
			if (key.length() == 0) {
				throw new RuntimeException("QString format illegal");
			}
			map.put(key, "");
		} else {
			if (key.length() == 0) {
				throw new RuntimeException("QString format illegal");
			}
			map.put(key, temp.toString());
		}
	}

}
