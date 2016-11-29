package net.aimeizi;

import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.model.Result;
import hudson.tasks.*;
import hudson.util.FormValidation;
import jenkins.model.Jenkins;
import net.sf.json.JSONObject;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.StaplerRequest;

import javax.servlet.ServletException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Kohsuke Kawaguchi
 */
public class NotifyQQ extends Notifier {

    /**
     * QQ号码列表
     */
    private final List<QQNumber> qQNumbers;
    /**
     * QQ消息内容
     */
    private final String qqmessage;
    /**
     * QQ服务器地址
     */
    private final String serverurl;
    private PrintStream logger;

    @DataBoundConstructor
    public NotifyQQ(String serverurl, List<QQNumber> qQNumbers, String qqmessage) {
        this.serverurl = serverurl;
        this.qQNumbers = new ArrayList<QQNumber>(qQNumbers);
        this.qqmessage = qqmessage;
    }

    public String getServerurl() {
        return serverurl;
    }

    public List<QQNumber> getQQNumbers() {
        return qQNumbers;
    }

    public String getQqmessage() {
        return qqmessage;
    }

    public BuildStepMonitor getRequiredMonitorService() {
        return BuildStepMonitor.NONE;
    }

    @Override
    public boolean perform(AbstractBuild build, Launcher launcher, BuildListener listener) throws UnsupportedEncodingException {

        logger = listener.getLogger();

        Jenkins.getInstance();
        String jobURL = "";
        try {
            jobURL = build.getEnvironment(listener).expand("${JOB_URL}");
            logger.println("jobURL = " + jobURL);
        } catch (Exception e) {
            logger.println("tokenmacro expand error.");
        }

        String msg = "各位小伙伴，项目";
        msg += build.getFullDisplayName();
        if (build.getResult() == Result.SUCCESS) {
            msg += "编译成功！" + qqmessage;
        } else {
            msg += "编译失败了...";
            msg += "jenkins地址:" + jobURL;
        }

        msg = URLEncoder.encode(msg, "UTF-8");
        msg = msg.replaceAll("\\+", "_");

        for (int i = 0; i < qQNumbers.size(); i++) {
            QQNumber number = qQNumbers.get(i);
            send(GenerateMessageURL(number.GetUrlString(), msg));
        }

        return true;
    }

    /**
     * 构建发送QQ消息url
     *
     * @param qq
     * @param msg
     * @return
     */
    private String GenerateMessageURL(String qq, String msg) {
        return String.format(this.getServerurl() + "/openqq/%s&content=%s", qq, msg);
    }

    /**
     * 发送QQ消息
     *
     * @param url
     */
    protected void send(String url) {
        logger.println("Sendurl: " + url);
        HttpURLConnection connection = null;
        InputStream is = null;
        String resultData = "";
        try {
            URL targetUrl = new URL(url);
            connection = (HttpURLConnection) targetUrl.openConnection();
            connection.setConnectTimeout(10 * 1000);
            connection.setReadTimeout(10 * 1000);
            connection.connect();
            is = connection.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bufferReader = new BufferedReader(isr);
            String inputLine = "";
            while ((inputLine = bufferReader.readLine()) != null) {
                resultData += inputLine + "\n";
            }
            logger.println("response: " + resultData);
        } catch (Exception e) {
            logger.println("http error." + e);
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        logger.println("Send url finish");
    }

    protected void sendAsync(String url) {
        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(100000)
                .setConnectTimeout(100000).build();
        CloseableHttpAsyncClient httpclient = HttpAsyncClients.custom().
                setDefaultRequestConfig(requestConfig)
                .build();
        try {
            httpclient.start();
            final HttpGet request = new HttpGet(url);
            httpclient.execute(request, new FutureCallback<HttpResponse>() {

                @Override
                public void completed(final HttpResponse response) {
                    logger.println(request.getRequestLine() + "->" + response.getStatusLine());
                }

                @Override
                public void failed(final Exception ex) {
                    logger.println(request.getRequestLine() + "->" + ex);
                }

                @Override
                public void cancelled() {
                    logger.println(request.getRequestLine() + " cancelled");
                }
            });
        } catch (Exception e) {
            logger.println("http error." + e);
        } finally {
            try {
                httpclient.close();
            } catch (Exception e) {
            }
        }
        logger.println("send Done");
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    @Extension
    public static final class DescriptorImpl extends
            BuildStepDescriptor<Publisher> {

        public DescriptorImpl() {
            load();
        }

        private boolean isNumeric(String str) {
            Pattern pattern = Pattern.compile("[0-9]*");
            Matcher isNum = pattern.matcher(str);
            if (!isNum.matches()) {
                return false;
            }
            return true;
        }

        /**
         * QQ号码合法性校验
         *
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckNumber(@QueryParameter String value)
                throws IOException, ServletException {
            if (value.length() <= 4)
                return FormValidation.error("你QQ号太短了吧。。。");
            else if (value.length() > 15)
                return FormValidation.error("QQ号有这么长吗？");
            else if (!isNumeric(value))
                return FormValidation.error("QQ号格式不对，数字数字数字！");
            return FormValidation.ok();
        }

        /**
         * QQ消息内容校验
         *
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckQqmessage(@QueryParameter String value)
                throws IOException, ServletException {
            return FormValidation.ok();
        }

        /**
         * QQ服务器地址校验
         *
         * @param value
         * @return
         * @throws IOException
         * @throws ServletException
         */
        public FormValidation doCheckServerurl(@QueryParameter String value)
                throws IOException, ServletException {
            return FormValidation.ok();
        }

        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        /**
         * jenkins中显示名称
         *
         * @return
         */
        public String getDisplayName() {
            return "QQ通知";
        }

        @Override
        public boolean configure(StaplerRequest req, JSONObject formData) throws FormException {
            save();
            return super.configure(req, formData);
        }

    }
}

