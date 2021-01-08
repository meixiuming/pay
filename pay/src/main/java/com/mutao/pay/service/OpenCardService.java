package com.mutao.pay.service;

import com.alibaba.fastjson.JSON;
import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import com.mutao.pay.sdk.SDKUtil;
import com.mutao.pay.util.DateUtil;
import com.mutao.pay.util.OrderNoCenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 14:17
 */
@Service
public class OpenCardService {
    @Value("${merId}")
    private String merId;

    public Map<String, String> openCard(String accNo) throws ServletException, IOException {
        String orderId = OrderNoCenter.getInstance().create();
        String txnTime = DateUtil.getTimeStrByDate(new Date());
        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", DemoBase.version);                   //版本号
        contentData.put("encoding", DemoBase.encoding);            //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "79");                              //交易类型 11-代收
        contentData.put("txnSubType", "00");                           //交易子类型 00-默认开通
        contentData.put("bizType", "000902");                          //业务类型 token支付
        contentData.put("channelType", "07");                          //渠道类型07-PC

        /***商户接入参数***/
        contentData.put("merId", merId);                               //商户号码（本商户号码仅做为测试调通交易使用，该商户号配置了需要对敏感信息加密）测试时请改成自己申请的商户号，【自己注册的测试777开头的商户号不支持代收产品】
        contentData.put("accessType", "0");                            //接入类型，商户接入固定填0，不需修改
        contentData.put("orderId", orderId);                           //商户订单号，如上送短信验证码，请填写获取验证码时一样的orderId，此处默认取demo演示页面传递的参数
        contentData.put("txnTime", txnTime);                           //订单发送时间，如上送短信验证码，请填写获取验证码时一样的txnTime，此处默认取demo演示页面传递的参数
        contentData.put("accType", "01");                              //账号类型

        //测试环境固定trId=99988877766&tokenType=01，生产环境由业务分配。测试环境因为所有商户都使用同一个trId，所以同一个卡获取的token号都相同，任一人发起更新token或者解除token请求都会导致原token号失效，所以之前成功、突然出现3900002报错时请先尝试重新开通一下。
        contentData.put("tokenPayData", "{trId=62000080009&tokenType=01}");

        //贷记卡 必送：卡号、手机号、CVN2、有效期；验证码看业务配置（默认不要短信验证码）。
        //借记卡 必送：卡号、手机号；选送：证件类型+证件号、姓名；验证码看业务配置（默认不要短信验证码）。
        //此测试商户号777290058110097 后台开通业务只支持 贷记卡
        Map<String, String> customerInfoMap = new HashMap<String, String>();
//        customerInfoMap.put("certifTp", "01");						//证件类型
//        customerInfoMap.put("certifId", "341126197709218366");		//证件号码
//        customerInfoMap.put("customerNm", "全渠道");					//姓名
        customerInfoMap.put("phoneNo", "18615269525");                    //手机号
        //customerInfoMap.put("pin", "123456");						    //密码【这里如果送密码 商户号必须配置 ”商户允许采集密码“】
//        customerInfoMap.put("cvn2", "123");                            //卡背面的cvn2三位数字
//        customerInfoMap.put("expired", "2311");                        //有效期 年在前月在后
//        customerInfoMap.put("smsCode", "111111");                        //短信验证码

        ////////////如果商户号开通了【商户对敏感信息加密】的权限那么需要对 accNo，pin和phoneNo，cvn2，expired加密（如果这些上送的话），对敏感信息加密使用：
        String accNo1 = AcpService.encryptData(accNo, "UTF-8");  //这里测试的时候使用的是测试卡号，正式环境请使用真实卡号
        contentData.put("accNo", accNo1);
        contentData.put("encryptCertId", AcpService.getEncryptCertId());       //加密证书的certId，配置在acp_sdk.properties文件 acpsdk.encryptCert.path属性下
        String customerInfoStr = AcpService.getCustomerInfoWithEncrypt(customerInfoMap, accNo1, DemoBase.encoding);
        //////////

        /////////如果商户号未开通【商户对敏感信息加密】权限那么不需对敏感信息加密使用：
        //contentData.put("accNo", "6221558812340000");            		//这里测试的时候使用的是测试卡号，正式环境请使用真实卡号
        //String customerInfoStr = AcpService.getCustomerInfo(customerInfoMap,"6221558812340000",DemoBase.encoding_UTF8);
        ////////

        contentData.put("customerInfo", customerInfoStr);
        contentData.put("backUrl", "http://180.169.111.154/bio/palmpay");

        // 请求方保留域，
        // 透传字段，查询、通知、对账文件中均会原样出现，如有需要请启用并修改自己希望透传的数据。
        // 出现部分特殊字符时可能影响解析，请按下面建议的方式填写：
        // 1. 如果能确定内容不会出现&={}[]"'等符号时，可以直接填写数据，建议的方法如下。
//		contentData.put("reqReserved", "透传信息1|透传信息2|透传信息3");
        // 2. 内容可能出现&={}[]"'符号时：
        // 1) 如果需要对账文件里能显示，可将字符替换成全角＆＝｛｝【】“‘字符（自己写代码，此处不演示）；
        // 2) 如果对账文件没有显示要求，可做一下base64（如下）。
        //    注意控制数据长度，实际传输的数据长度不能超过1024位。
        //    查询、通知等接口解析时使用new String(Base64.decodeBase64(reqReserved), DemoBase.encoding);解base64后再对数据做后续解析。
//		contentData.put("reqReserved", Base64.encodeBase64String("任意格式的信息都可以".toString().getBytes(DemoBase.encoding)));

        /**对请求参数进行签名并发送http post请求，接收同步应答报文**/
        Map<String, String> reqData = AcpService.sign(contentData, DemoBase.encoding);              //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();                                  //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData, requestBackUrl, DemoBase.encoding);  //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        StringBuffer parseStr = new StringBuffer("");
        if (!rspData.isEmpty()) {
            if (AcpService.validate(rspData, DemoBase.encoding)) {
                LogUtil.writeLog("验证签名成功");
                String respCode = rspData.get("respCode");
                if (("00").equals(respCode)) {
                    //成功
                    parseStr.append("<br>解析敏感信息加密信息如下（如果有）:<br>");
                    String customerInfo = rspData.get("customerInfo");
                    if (null != customerInfo) {
                        Map<String, String> cm = AcpService.parseCustomerInfo(customerInfo, "UTF-8");
                        parseStr.append("customerInfo明文: " + cm + "<br>");
                    }
                    //如果是配置了敏感信息加密，如果需要获取卡号的明文，可以按以下方法解密卡号
                    String an = rspData.get("accNo");
                    if (null != an) {
                        an = AcpService.decryptData(an, "UTF-8");
                        parseStr.append("accNo明文: " + an);
                    }

                    String tokenPayData = rspData.get("tokenPayData");
                    if (null != tokenPayData) {
                        Map<String, String> tokenPayDataMap = SDKUtil.parseQString(tokenPayData.substring(1, tokenPayData.length() - 1));
                        String token = tokenPayDataMap.get("token");//这样取
                        parseStr.append("tokenPayDataMap明文: " + tokenPayDataMap);
                    }
                    //TODO
                } else {
                    //其他应答码为失败请排查原因或做失败处理
                    //TODO
                }
            } else {
                LogUtil.writeErrorLog("验证签名失败");
                //TODO 检查验证签名失败的原因
            }
        } else {
            //未返回正确的http状态
            LogUtil.writeErrorLog("未获取到返回报文或返回http状态码非200");
        }
        LogUtil.writeLog("请求报文："+JSON.toJSONString(reqData));
        LogUtil.writeLog("返回报文："+JSON.toJSONString(rspData));

        String reqMessage = DemoBase.genHtmlResult(reqData);
        String rspMessage = DemoBase.genHtmlResult(rspData);

        Map<String, String> result = new HashMap<>();
        result.put("reqMessage",reqMessage);
        result.put("rspMessage",rspMessage);
        result.put("parseStr",parseStr.toString());
        return result;
    }
}
