package asia.lhweb.findfriend.config;

import asia.lhweb.findfriend.common.JacksonObjectMapper;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.text.SimpleDateFormat;
import java.util.List;

import static asia.lhweb.findfriend.constants.SystemConstants.CROSS_ORIGIN_ALLOWED_TIME;

/**
 * web mvc配置
 *
 * @author 罗汉
 * @date 2023/07/28
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    /**
     * 扩展消息转换器
     *
     * @param converters 转换器
     */
    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        JacksonObjectMapper objectMapper = new JacksonObjectMapper();
        //重写日期格式
        SimpleDateFormat smt = new SimpleDateFormat("yyyy-MM-dd");
        objectMapper.setDateFormat(smt);
        converter.setObjectMapper(objectMapper);
        converters.add(0, converter);
    }

    /**
     * 添加歌珥映射
     *
     * @param registry 注册表
     */
    // @Override
    // public void addCorsMappings(CorsRegistry registry) {
    //     registry.addMapping("/**")
    //             //设置允许跨域请求的域名
    //             //当**Credentials为true时，**Origin不能为星号，需为具体的ip地址【如果接口不带cookie,ip无需设成具体ip】
    //             // .allowedOrigins("http://localhost:5173", "http://localhost:5174")
    //             .allowedOrigins("http://127.0.0.1:5173", "http://127.0.0.1:5174")
    //             //是否允许证书 不再默认开启
    //             .allowCredentials(true)
    //             //设置允许的方法
    //             .allowedMethods("*")
    //             //跨域允许时间
    //             .maxAge(CROSS_ORIGIN_ALLOWED_TIME);
    // }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        //覆盖所有请求
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOriginPatterns("*")
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("*");
    }
}
