package com.mutao.pay.service;

import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import com.mutao.pay.util.DateUtil;
import com.mutao.pay.util.OrderNoCenter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 17:12
 */
@Service
public class OpenCardFrontService {
    @Value("${merId}")
    private String merId;

    public Map<String, String> openCardFront(String accNo,String phoneNo) throws ServletException, IOException {
        String orderId = OrderNoCenter.getInstance().create();
        String txnTime = DateUtil.getTimeStrByDate(new Date());

        Map<String, String> contentData = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        contentData.put("version", DemoBase.version);                  //版本号
        contentData.put("encoding", DemoBase.encoding);            //字符集编码 可以使用UTF-8,GBK两种方式
        contentData.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        contentData.put("txnType", "79");                              //交易类型 11-代收
        contentData.put("txnSubType", "00");                           //交易子类型 00-默认开通
        contentData.put("bizType", "000902");                          //业务类型 Token支付
        contentData.put("channelType", "07");                          //渠道类型07-PC

        /***商户接入参数***/
        contentData.put("merId", merId);                               //商户号码（本商户号码仅做为测试调通交易使用，该商户号配置了需要对敏感信息加密）测试时请改成自己申请的商户号，【自己注册的测试777开头的商户号不支持代收产品】
        contentData.put("accessType", "0");                            //接入类型，商户接入固定填0，不需修改
        contentData.put("orderId", orderId);                           //商户订单号，8-40位数字字母，不能含“-”或“_”，可以自行定制规则
        contentData.put("txnTime", txnTime);                           //订单发送时间，格式为yyyyMMddHHmmss，必须取当前时间，否则会报txnTime无效
        contentData.put("accType", "01");                              //账号类型

        //测试环境固定trId=99988877766&tokenType=01，生产环境由业务分配。测试环境因为所有商户都使用同一个trId，所以同一个卡获取的token号都相同，任一人发起更新token或者解除token请求都会导致原token号失效，所以之前成功、突然出现3900002报错时请先尝试重新开通一下。
        contentData.put("tokenPayData", "{trId=99988877766&tokenType=01}");

        //只支持贷记卡 必送：卡号、手机号、CVN2、有效期；验证码看业务配置（默认不要短信验证码,本测试商户777290058110097配置了需要）。
//		Map<String,String> customerInfoMap = new HashMap<String,String>();
//		customerInfoMap.put("phoneNo", "13552535506");			        //手机号
//		customerInfoMap.put("cvn2", "123");           			        //卡背面的cvn2三位数字
//		customerInfoMap.put("expired", "2311");  				        //有效期 年在前月在后
//		customerInfoMap.put("certifTp", "01");						//证件类型
//		customerInfoMap.put("certifId", "341126197709218366");		//证件号码

        //选送卡号、手机号、证件类型+证件号、姓名
        //也可以都不送,在界面输入这些要素
        //此测试商户号777290058110097 后台开通业务只支持 贷记卡
        Map<String, String> customerInfoMap = new HashMap<String, String>();
//        customerInfoMap.put("certifTp", "01");                        //证件类型
//        customerInfoMap.put("certifId", "341126197709218366");        //证件号码
//        customerInfoMap.put("customerNm", "全渠道");                    //姓名
        customerInfoMap.put("phoneNo", phoneNo);                //手机号

        ////////////如果商户号开通了【商户对敏感信息加密】的权限那么需要对 accNo，pin和phoneNo，cvn2，expired加密（如果这些上送的话），对敏感信息加密使用：
        String accNo1 = AcpService.encryptData(accNo, "UTF-8");  //这里测试的时候使用的是测试卡号，正式环境请使用真实卡号
        contentData.put("accNo", accNo1);
        contentData.put("encryptCertId", AcpService.getEncryptCertId());       //加密证书的certId，配置在acp_sdk.properties文件 acpsdk.encryptCert.path属性下
        String customerInfoStr = AcpService.getCustomerInfoWithEncrypt(customerInfoMap, null, DemoBase.encoding);
        //////////

        /////////如果商户号未开通【商户对敏感信息加密】权限那么不需对敏感信息加密使用：
        //contentData.put("accNo", "6216261000000000018");
        //String customerInfoStr = AcpService.getCustomerInfo(customerInfoMap,null,DemoBase.encoding_UTF8);   //前台实名认证送支付验证要素 customerInfo中要素不要加密
        ////////
        contentData.put("customerInfo", customerInfoStr);

        //前台通知地址 （需设置为外网能访问 http https均可），支付成功后的页面 点击“返回商户”的时候将异步通知报文post到该地址
        //如果想要实现过几秒中自动跳转回商户页面权限，需联系银联业务申请开通自动返回商户权限
        //注：如果开通失败的“返回商户”按钮也是触发frontUrl地址，点击时是按照get方法返回的，没有通知数据返回商户
        contentData.put("frontUrl", DemoBase.frontUrl);

        //后台通知地址（需设置为【外网】能访问 http https均可），支付成功后银联会自动将异步通知报文post到商户上送的该地址，失败的交易银联不会发送后台通知
        //后台通知参数详见open.unionpay.com帮助中心 下载  产品接口规范  网关支付产品接口规范 消费交易 商户通知
        //注意:1.需设置为外网能访问，否则收不到通知    2.http https均可  3.收单后台通知后需要10秒内返回http200或302状态码
        //    4.如果银联通知服务器发送通知后10秒内未收到返回状态码或者应答码非http200，那么银联会间隔一段时间再次发送。总共发送5次，每次的间隔时间为0,1,2,4分钟。
        //    5.后台通知地址如果上送了带有？的参数，例如：http://abc/web?a=b&c=d 在后台通知处理程序验证签名之前需要编写逻辑将这些字段去掉再验签，否则将会验签失败
        contentData.put("backUrl", DemoBase.backUrl);

        // contentData.put("reserved", "{customPage=true}");         	//如果开通页面需要使用嵌入页面的话，请上送此用法

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

        // 订单超时时间。
        // 超过此时间后，除网银交易外，其他交易银联系统会拒绝受理，提示超时。 跳转银行网银交易如果超时后交易成功，会自动退款，大约5个工作日金额返还到持卡人账户。
        // 此时间建议取支付时的北京时间加15分钟。
        // 超过超时时间调查询接口应答origRespCode不是A6或者00的就可以判断为失败。
        contentData.put("payTimeout", new SimpleDateFormat("yyyyMMddHHmmss").format(new Date().getTime() + 15 * 60 * 1000));


        /**请求参数设置完毕，以下对请求参数进行签名并生成html表单，将表单写入浏览器跳转打开银联页面**/
        Map<String, String> reqData = AcpService.sign(contentData, DemoBase.encoding);             //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String requestFrontUrl = SDKConfig.getConfig().getFrontRequestUrl();                             //获取请求银联的前台地址：对应属性文件acp_sdk.properties文件中的acpsdk.frontTransUrl
        String html = AcpService.createAutoFormHtml(requestFrontUrl, reqData, DemoBase.encoding);     //生成自动跳转的Html表单

        LogUtil.writeLog("打印请求HTML，此为请求报文，为联调排查问题的依据：" + html);

        Map<String, String> result = new HashMap<>();
        result.put("html", html);
        return result;
    }
}
