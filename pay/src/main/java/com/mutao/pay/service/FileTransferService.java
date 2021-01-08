package com.mutao.pay.service;

import com.mutao.pay.demo.DemoBase;
import com.mutao.pay.sdk.AcpService;
import com.mutao.pay.sdk.LogUtil;
import com.mutao.pay.sdk.SDKConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author mutao
 * @version 1.0.0
 * @date 2020/12/2 15:37
 */
@Service
public class FileTransferService {
    @Value("${merId}")
    private String merId;

    public Map<String, String> fileTransfer(String settleDate) throws ServletException, IOException {

        Map<String, String> data = new HashMap<String, String>();

        /***银联全渠道系统，产品参数，除了encoding自行选择外其他不需修改***/
        data.put("version", DemoBase.version);               //版本号 全渠道默认值
        data.put("encoding", DemoBase.encoding);             //字符集编码 可以使用UTF-8,GBK两种方式
        data.put("signMethod", SDKConfig.getConfig().getSignMethod()); //签名方法
        data.put("txnType", "76");                           //交易类型 76-对账文件下载
        data.put("txnSubType", "01");                        //交易子类型 01-对账文件下载
        data.put("bizType", "000000");                       //业务类型，固定

        /***商户接入参数***/
        data.put("accessType", "0");                         //接入类型，商户接入填0，不需修改
        data.put("merId", merId);                	         //商户代码，请替换正式商户号测试，如使用的是自助化平台注册的777开头的商户号，该商户号没有权限测文件下载接口的，请使用测试参数里写的文件下载的商户号和日期测。如需777商户号的真实交易的对账文件，请使用自助化平台下载文件。
        data.put("settleDate", settleDate);                  //清算日期，如果使用正式商户号测试则要修改成自己想要获取对账文件的日期， 测试环境如果使用700000000000001商户号则固定填写0119
        data.put("txnTime",DemoBase.getCurrentTime());       //订单发送时间，取系统时间，格式为yyyyMMddHHmmss，必须取当前时间，否则会报txnTime无效
        data.put("fileType", "00");                          //文件类型，一般商户填写00即可

        /**请求参数设置完毕，以下对请求参数进行签名并发送http post请求，接收同步应答报文------------->**/

        Map<String, String> reqData = AcpService.sign(data,DemoBase.encoding);				//报文中certId,signature的值是在signData方法中获取并自动赋值的，只要证书配置正确即可。
        String url = SDKConfig.getConfig().getFileTransUrl();										//获取请求银联的前台地址：对应属性文件acp_sdk.properties文件中的acpsdk.fileTransUrl
        Map<String, String> rspData = AcpService.post(reqData, url,DemoBase.encoding);

        /**对应答码的处理，请根据您的业务逻辑来编写程序,以下应答码处理逻辑仅供参考------------->**/

        //应答码规范参考open.unionpay.com帮助中心 下载  产品接口规范  《平台接入接口规范-第5部分-附录》
        String fileContentDispaly = "";
        if(!rspData.isEmpty()){
            if(AcpService.validate(rspData, DemoBase.encoding)){
                LogUtil.writeLog("验证签名成功");
                String respCode = rspData.get("respCode");
                if("00".equals(respCode)){
                    //交易成功，解析返回报文中的fileContent并落地
                    String zipFilePath = AcpService.deCodeFileContent(rspData,"d:\\",DemoBase.encoding);
                    //对落地的zip文件解压缩并解析
                    String outPutDirectory ="d:\\";
                    List<String> fileList = DemoBase.unzip(zipFilePath, outPutDirectory);
                    //解析ZM，ZME文件
                    for(String file : fileList){
                        if(file.indexOf("ZM_")!=-1){
                            List<Map> ZmDataList = DemoBase.parseZMFile(file);
                            fileContentDispaly = DemoBase.getFileContentTable(ZmDataList,file);
                        }else if(file.indexOf("ZME_")!=-1){
                            DemoBase.parseZMEFile(file);
                        }
                    }
                    //TODO
                }else{
                    //其他应答码为失败请排查原因
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

        String reqMessage = DemoBase.genHtmlResult(reqData);
        String rspMessage = DemoBase.genHtmlResult(rspData);
        //对账文件内容

        Map<String, String> result = new HashMap<>();
        result.put("reqMessage", reqMessage);
        result.put("rspMessage", rspMessage);
        return result;
    }
}

