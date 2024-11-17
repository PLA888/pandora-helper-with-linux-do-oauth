//package fun.yeelo.oauth.utils;
//
//import org.openqa.selenium.*;
//import org.openqa.selenium.chrome.*;
//import org.openqa.selenium.support.ui.*;
//import io.github.bonigarcia.wdm.WebDriverManager;
//
//import java.io.IOException;
//import java.nio.file.Files;
//import java.time.Duration;
//import java.io.File;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import org.json.JSONObject;
//import org.springframework.core.io.ClassPathResource;
//import org.springframework.util.FileSystemUtils;
//
//public class ChatGPTLoginAutomation {
//    private WebDriver driver;
//    private WebDriverWait wait;
//    private final String account = "kyle4@yeelo.fun";
//    private final String password = "PmS1EGrHeey84km";
//    private static final String EXTENSION_PATH = "turnstilePatch";
//    private File extensionDir;
//
//    public void setup() {
//        try {
//            // 使用特定版本的 ChromeDriver
//            WebDriverManager.chromedriver().browserVersion("127").setup();
//
//            ChromeOptions options = new ChromeOptions();
//
//            // 从resources目录复制扩展到临时目录
//            extensionDir = Files.createTempDirectory("turnstilePatch").toFile();
//            ClassPathResource resource = new ClassPathResource("turnstilePatch");
//            FileSystemUtils.copyRecursively(resource.getFile(), extensionDir);
//
//            System.out.println("扩展路径: " + extensionDir.getAbsolutePath());
//
//            // 添加必要的 Chrome 选项
//            options.addArguments("--load-extension=" + extensionDir.getAbsolutePath());
//            options.addArguments("--remote-allow-origins=*");
//            options.addArguments("--no-sandbox");
//            options.addArguments("--disable-dev-shm-usage");
//            options.addArguments("--disable-gpu");
//            // options.addArguments("--headless=new"); // 暂时注释掉 headless 模式
//            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
//
//            // 禁用日志
//            System.setProperty("webdriver.chrome.silentOutput", "true");
//            java.util.logging.Logger.getLogger("org.openqa.selenium").setLevel(java.util.logging.Level.OFF);
//
//            Map<String, Object> prefs = new HashMap<>();
//            prefs.put("credentials_enable_service", Optional.of(false));
//            prefs.put("profile.default_content_setting_values.notifications", Optional.of(2));
//            options.setExperimentalOption("prefs", prefs);
//
//            // 禁用扩展错误弹窗
//            options.addArguments("--disable-popup-blocking");
//            options.addArguments("--disable-extensions-except=" + extensionDir.getAbsolutePath());
//            options.addArguments("--disable-notifications");
//
//            // 创建 ChromeDriver 实例
//            driver = new ChromeDriver(options);
//            driver.manage().window().maximize();
//            wait = new WebDriverWait(driver, Duration.ofSeconds(10));
//
//        } catch (IOException e) {
//            throw new RuntimeException("设置扩展失败", e);
//        }
//    }
//    private void handleTurnstileToken() {
//        try {
//            System.out.println("\n等待验证框加载...");
//            WebElement challengeSolution = wait.until(ExpectedConditions.presenceOfElementLocated(By.name("cf-turnstile-response")));
//            if (challengeSolution != null) {
//                System.out.println("\n验证框加载完成");
//                WebElement challengeWrapper = challengeSolution.findElement(By.xpath("./.."));
//                WebElement challengeIframe = challengeWrapper.getShadowRoot().findElement(By.tagName("iframe"));
//                driver.switchTo().frame(challengeIframe);
//                WebElement challengeButton = driver.findElement(By.tagName("input"));
//                challengeButton.click();
//                driver.switchTo().defaultContent();
//                System.out.println("\n验证按钮已点击，等待验证完成...");
//            }
//        } catch (Exception e) {
//            System.out.println("处理验证失败: " + e.getMessage());
//        }
//    }
//
//    public void login() {
//        try {
//            System.out.println("步骤1: 开始访问网站...");
//            driver.get("https://chatgpt.com");
//
//            // 等待页面加载
//            try {
//                wait.until(new ExpectedCondition<Boolean>() {
//                    @Override
//                    public Boolean apply(WebDriver d) {
//                        try {
//                            return (Boolean) (d.findElement(By.tagName("textarea")) != null ||
//                                                                       d.findElement(By.cssSelector(".btn.relative.btn-blue.btn-large")) != null);
//                        } catch (Exception e) {
//                            return (Boolean) false;
//                        }
//                    }
//                });
//                System.out.println("\n页面加载完成");
//
//                if (driver.findElements(By.name("cf-turnstile-response")).size() > 0) {
//                    System.out.println("\n准备处理验证框...");
//                    handleTurnstileToken();
//                }
//            } catch (Exception e) {
//                System.out.println("\n加载登录页面出错: " + e.getMessage());
//            }
//
//            System.out.println("\n步骤2: 开始登录...");
//            // 点击登录按钮
//            try {
//                WebElement signinBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
//                        By.cssSelector(".btn.relative.btn-primary.btn-small")));
//                System.out.println("\n找到黑色登录按钮: " + signinBtn.getText());
//                signinBtn.click();
//
//                WebElement signupBtn = wait.until(ExpectedConditions.presenceOfElementLocated(
//                        By.cssSelector(".btn.relative.btn-secondary.btn-small")));
//                System.out.println("\n找到注册按钮: " + signupBtn.getText());
//                signupBtn.click();
//
//                WebElement signinLink = wait.until(ExpectedConditions.presenceOfElementLocated(
//                        By.className("other-page-link")));
//                System.out.println("\n找到跳转登录链接: " + signinLink.getText());
//                signinLink.click();
//            } catch (Exception e) {
//                System.out.println("处理登录按钮时出错: " + e.getMessage());
//            }
//
//            System.out.println("\n步骤3: 输入邮箱...");
//            try {
//                WebElement emailInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("email-input")));
//                System.out.println("\n邮箱输入框加载完成");
//                emailInput.sendKeys(account);
//                Thread.sleep(500);
//                driver.findElement(By.className("continue-btn")).click();
//                System.out.println("\n输入邮箱并点击继续");
//            } catch (Exception e) {
//                System.out.println("加载邮箱输入框时出错: " + e.getMessage());
//            }
//
//            System.out.println("\n步骤4: 输入密码...");
//            try {
//                WebElement titleWrapper = wait.until(ExpectedConditions.presenceOfElementLocated(
//                        By.className("title-wrapper")));
//                if (titleWrapper.getText().contains("获取您的 SSO 信息时出错")) {
//                    System.out.println("\n检测到 SSO 错误，脚本终止，请手动登录");
//                    return;
//                }
//
//                WebElement passwordInput = wait.until(ExpectedConditions.presenceOfElementLocated(By.id("password")));
//                System.out.println("\n密码输入框加载完成");
//                passwordInput.sendKeys(password);
//                Thread.sleep(2000);
//                driver.findElement(By.cssSelector("button[type='submit']")).click();
//                System.out.println("\n输入密码并点击登录");
//            } catch (Exception e) {
//                System.out.println("输入密码时出错: " + e.getMessage());
//            }
//
//            System.out.println("\n步骤5: 获取access_token...");
//            try {
//                wait.until(ExpectedConditions.presenceOfElementLocated(
//                        By.xpath("//*[contains(text(), '有什么可以帮忙的？')]")));
//                System.out.println("\n登录成功！");
//
//                if (driver.findElements(By.xpath("//*[contains(text(), '重新发送电子邮件')]")).size() > 0) {
//                    System.out.println("\n提示需要邮箱验证码，脚本终止，请手动获取");
//                    return;
//                }
//
//                // 获取access token
//                ((JavascriptExecutor) driver).executeScript("window.open()");
//                driver.switchTo().window(driver.getWindowHandles().toArray(new String[0])[1]);
//                driver.get("https://chatgpt.com/api/auth/session");
//                Thread.sleep(1000);
//
//                String pageSource = driver.getPageSource();
//                JSONObject jsonResponse = new JSONObject(pageSource);
//                if (jsonResponse.has("accessToken")) {
//                    String accessToken = jsonResponse.getString("accessToken");
//                    System.out.println("\n请复制保存你的access_token:\n");
//                    System.out.println(accessToken);
//                } else {
//                    System.out.println("错误:未找到access token");
//                }
//            } catch (Exception e) {
//                System.out.println("获取access token时出错: " + e.getMessage());
//            }
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    public void cleanup() {
//        if (driver != null) {
//            System.out.println("\n按Enter键关闭浏览器...");
//            try {
//                System.in.read();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//            driver.quit();
//        }
//    }
//
//    public static void main(String[] args) throws IOException {
//        ChatGPTLoginAutomation automation = new ChatGPTLoginAutomation();
//        automation.setup();
//        automation.login();
//        automation.cleanup();
//    }
//}