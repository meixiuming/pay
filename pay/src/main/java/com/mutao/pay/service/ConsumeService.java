package com.mutao.pay.service;

import com.alibaba.fastjson.JSONObject;
import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import com.mutao.pay.util.DateUtil;
import com.mutao.pay.util.OrderNoCenter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 15:14
 */
@Service
public class ConsumeService {
    Logger logger = LoggerFactory.getLogger(ConsumeService.class);


    @Value("${merId}")
    private String merId;

    public Map<String, String> consume(String txnAmt, String token,String smsCode,String orderId,String txnTime) throws ServletException, IOException {
//        String orderId = OrderNoCenter.getInstance().create();
//        String txnTime = DateUtil.getTimeStrByDate(new Date());

        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", DemoBase.version);                  //版本号
        contentData.put("encoding", DemoBase.encoding);                //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "01");                              //交易类型 01-消费
        contentData.put("txnSubType", "01");                           //交易子类型 01-消费
        contentData.put("bizType", "000902");                          //业务类型 认证支付2.0
        contentData.put("channelType", "07");                          //渠道类型07-PC

        /***商户接入参数***/
        contentData.put("merId", merId);                               //商户号码（本商户号码仅做为测试调通交易使用，该商户号配置了需要对敏感信息加密）测试时请改成自己申请的商户号，【自己注册的测试777开头的商户号不支持代收产品】
        contentData.put("accessType", "0");                            //接入类型，商户接入固定填0，不需修改
        contentData.put("orderId", orderId);                           //商户订单号，如上送短信验证码，请填写获取验证码时一样的orderId，此处默认取demo演示页面传递的参数
        contentData.put("txnTime", txnTime);                           //订单发送时间，如上送短信验证码，请填写获取验证码时一样的txnTime，此处默认取demo演示页面传递的参数
        contentData.put("currencyCode", "156");                           //交易币种（境内商户一般是156 人民币）
        contentData.put("txnAmt", txnAmt);                               //交易金额，单位分，如上送短信验证码，请填写获取验证码时一样的txnAmt，此处默认取demo演示页面传递的参数
        contentData.put("accType", "01");                              //账号类型

        //后台通知地址（需设置为【外网】能访问 http https均可），支付成功后银联会自动将异步通知报文post到商户上送的该地址，失败的交易银联不会发送后台通知
        //后台通知参数详见open.unionpay.com帮助中心 下载  产品接口规范  代收产品接口规范 代收交易 商户通知
        //注意:1.需设置为外网能访问，否则收不到通知    2.http https均可  3.收单后台通知后需要10秒内返回http200或302状态码
        //    4.如果银联通知服务器发送通知后10秒内未收到返回状态码或者应答码非http200，那么银联会间隔一段时间再次发送。总共发送5次，每次的间隔时间为0,1,2,4分钟。
        //    5.后台通知地址如果上送了带有？的参数，例如：http://abc/web?a=b&c=d 在后台通知处理程序验证签名之前需要编写逻辑将这些字段去掉再验签，否则将会验签失败
        contentData.put("backUrl", DemoBase.backUrl);

        //消费：token号（从前台开通的后台通知中获取或者后台开通的返回报文中获取），验证码看业务配置(默认要短信验证码)。
//        contentData.put("tokenPayData", "{token=" + token + "&trId=99988877766}");
        contentData.put("tokenPayData", "{token=" + token + "&trId=62000002975}");

        Map<String, String> customerInfoMap = new HashMap<String, String>();
        customerInfoMap.put("smsCode", smsCode);                    //短信验证码
        //customerInfoMap不送pin的话 该方法可以不送 卡号
        String customerInfoStr = AcpService.getCustomerInfo(customerInfoMap, null, DemoBase.encoding);
        contentData.put("customerInfo", customerInfoStr);

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

        //分期付款用法（商户自行设计分期付款展示界面）：
        //修改txnSubType=03，增加instalTransInfo域
        //【测试环境】固定使用测试卡号6221558812340000，测试金额固定在100-1000元之间，分期数固定填06
        //【生产环境】支持的银行列表清单请联系银联业务运营接口人索要
        //contentData.put("txnSubType", "03");                           //交易子类型 03-分期付款
        //contentData.put("instalTransInfo","{numberOfInstallments=06}");//分期付款信息域，numberOfInstallments期数

        /**对请求参数进行签名并发送http post请求，接收同步应答报文**/
        Map<String, String> reqData = AcpService.sign(contentData, DemoBase.encoding);                //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestBackUrl = SDKConfig.getConfig().getBackRequestUrl();                            //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.backTransUrl
        Map<String, String> rspData = AcpService.post(reqData, requestBackUrl, DemoBase.encoding);    //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        StringBuffer parseStr = new StringBuffer("");
        if (!rspData.isEmpty()) {
            if (AcpService.validate(rspData, DemoBase.encoding)) {
                LogUtil.writeLog("验证签名成功");
                String respCode = rspData.get("respCode");
                if (("00").equals(respCode)) {
                    //交易已受理(不代表交易已成功），等待接收后台通知更新订单状态,也可以主动发起 查询交易确定交易状态。
                    //TODO
                    //如果是配置了敏感信息加密，如果需要获取卡号的铭文，可以按以下方法解密卡号
                    String accNo1 = rspData.get("accNo");
//					String accNo2 = AcpService.decryptData(accNo1, "UTF-8");  //解密卡号使用的证书是商户签名私钥证书acpsdk.signCert.path
//					LogUtil.writeLog("解密后的卡号："+accNo2);
//					parseStr.append("解密后的卡号："+accNo2);

                } else if (("03").equals(respCode) ||
                        ("04").equals(respCode) ||
                        ("05").equals(respCode)) {
                    //后续需发起交易状态查询交易确定交易状态
                    //TODO
                } else {
                    //其他应答码为失败请排查原因
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
        logger.info("消费请求报文"+ JSONObject.toJSONString(reqData));
        logger.info("消费返回报文"+JSONObject.toJSONString(rspData));
        String reqMessage = DemoBase.genHtmlResult(reqData);
        String rspMessage = DemoBase.genHtmlResult(rspData);
        Map<String, String> result = new HashMap<>();
        result.put("reqMessage", reqMessage);
        result.put("rspMessage", rspMessage);
        result.put("parseStr", parseStr.toString());
        return result;
    }
}
