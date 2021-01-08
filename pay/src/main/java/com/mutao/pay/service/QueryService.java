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
 * @date 2020/12/2 15:36
 */
@Service
public class QueryService {
    Logger logger = LoggerFactory.getLogger(QueryService.class);

    @Value("${merId}")
    private String merId;

    public Map<String, String> query(String orderId,String txnTime) throws ServletException, IOException {
        Map<String, String> data = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        data.put("version", DemoBase.version);                 //版本号
        data.put("encoding", DemoBase.encoding);               //字符集编码 可以使用UTF-8,GBK两种方式
        data.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        data.put("txnType", "00");                             //交易类型 00-默认
        data.put("txnSubType", "00");                          //交易子类型  默认00
        data.put("bizType", "000902");                         //业务类型

        /***商户接入参数***/
        data.put("merId", merId);                  			   //商户号码，请改成自己申请的商户号或者open上注册得来的777商户号测试
        data.put("accessType", "0");                           //接入类型，商户接入固定填0，不需修改

        /***要调通交易以下字段必须修改***/
        data.put("orderId", orderId);                 //****商户订单号，每次发交易测试需修改为被查询的交易的订单号
        data.put("txnTime", txnTime);                 //****订单发送时间，每次发交易测试需修改为被查询的交易的订单发送时间

        /**请求参数设置完毕，以下对请求参数进行签名并发送http post请求，接收同步应答报文------------->**/

        Map<String, String> reqData = AcpService.sign(data,DemoBase.encoding);           //报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String url = SDKConfig.getConfig().getSingleQueryUrl();						          //交易请求url从配置文件读取对应属性文件acp_sdk.properties中的 acpsdk.singleQueryUrl
        Map<String, String> rspData = AcpService.post(reqData,url,DemoBase.encoding);     //发送请求报文并接受同步应答（默认连接超时时间30秒，读取返回结果超时时间30秒）;这里调用signData之后，调用submitUrl之前不能对submitFromData中的键值对做任何修改，如果修改会导致验签不通过

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/
        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        if(!rspData.isEmpty()){
            if(AcpService.validate(rspData, DemoBase.encoding)){
                LogUtil.writeLog("验证签名成功");
                if(("00").equals(rspData.get("respCode"))){//如果查询交易成功
                    String origRespCode = rspData.get("origRespCode");
                    if(("00").equals(origRespCode)){
                        //交易成功，更新商户订单状态
                        //TODO
                    }else if(("03").equals(origRespCode)||
                            ("04").equals(origRespCode)||
                            ("05").equals(origRespCode)){
                        //订单处理中或交易状态未明，需稍后发起交易状态查询交易 【如果最终尚未确定交易是否成功请以对账文件为准】
                        //TODO
                    }else{
                        //其他应答码为交易失败
                        //TODO
                    }
                }else if(("34").equals(rspData.get("respCode"))){
                    //订单不存在，可认为交易状态未明，需要稍后发起交易状态查询，或依据对账结果为准

                }else{//查询交易本身失败，如应答码10/11检查查询报文是否正确
                    //TODO
                }
            }else{
                LogUtil.writeErrorLog("验证签名失败");
                //TODO 检查验证签名失败的原因
            }
        }else{
            //未返回正确的http状态
            LogUtil.writeErrorLog("未获取到返回报文或返回http状态码非200");
        }

        logger.info("查询请求报文"+ JSONObject.toJSONString(reqData));
        logger.info("查询返回报文"+JSONObject.toJSONString(rspData));
        String reqMessage = DemoBase.genHtmlResult(reqData);
        String rspMessage = DemoBase.genHtmlResult(rspData);


        Map<String, String> result = new HashMap<>();
        result.put("reqMessage", reqMessage);
        result.put("rspMessage", rspMessage);
        return result;
    }
}
