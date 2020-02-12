package com.ssafy.coco.service.impl;

import io.jsonwebtoken.*;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.spec.SecretKeySpec;
import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.DatatypeConverter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.coco.dao.MemberDao;
import com.ssafy.coco.service.JwtService;
import com.ssafy.coco.vo.Member;
import com.ssafy.coco.vo.Tokens;

@Service
public class JwtServiceImpl implements JwtService{

	private String secretKey = "ssafyisbest";

    private Logger logger = LoggerFactory.getLogger(JwtServiceImpl.class);

    @Autowired
    MemberDao memberDao;
    
    public JsonNode getAccessToken(String autorize_code) {
		final String RequestUrl = "https://kauth.kakao.com/oauth/token";

		final List<NameValuePair> postParams = new ArrayList<NameValuePair>();
		postParams.add(new BasicNameValuePair("grant_type", "authorization_code"));
		postParams.add(new BasicNameValuePair("client_id", "716ea071847daf5fdddd8ecac5cd2796")); // REST API KEY
		postParams.add(new BasicNameValuePair("redirect_uri", "http://192.168.100.94:8080")); // 리다이렉트 URI
		postParams.add(new BasicNameValuePair("code", autorize_code)); // 로그인 과정중 얻은 code 값
		//http://192.168.100.94:8080
		//http://192.168.100.95:8888/test/kakaologin2
		final HttpClient client = HttpClientBuilder.create().build();
		final HttpPost post = new HttpPost(RequestUrl);
		JsonNode returnNode = null;

		try {
			post.setEntity(new UrlEncodedFormEntity(postParams));
			final HttpResponse response = client.execute(post);
			final int responseCode = response.getStatusLine().getStatusCode();

			System.out.println("\nSending 'POST' request to URL : " + RequestUrl);
			System.out.println("Post parameters : " + postParams);
			System.out.println("Response Code : " + responseCode);

			// JSON 형태 반환값 처리
			ObjectMapper mapper = new ObjectMapper();
			returnNode = mapper.readTree(response.getEntity().getContent());

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// clear resources
		}

		return returnNode;

	}

	public JsonNode getKakaoUserInfo(String token) {

		final String RequestUrl = "https://kapi.kakao.com/v1/user/me";

		final HttpClient client = HttpClientBuilder.create().build();
		final HttpPost post = new HttpPost(RequestUrl);

		// add header
		post.addHeader("Authorization", "Bearer " + token);

		JsonNode returnNode = null;

		try {
			final HttpResponse response = client.execute(post);
			final int responseCode = response.getStatusLine().getStatusCode();

			System.out.println("\nSending 'POST' request to URL : " + RequestUrl);
			System.out.println("Response Code : " + responseCode);

			// JSON 형태 반환값 처리
			ObjectMapper mapper = new ObjectMapper();
			returnNode = mapper.readTree(response.getEntity().getContent());

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			// clear resources
		}
		return returnNode;

	}

	public Member changeData(JsonNode userInfo) {
		Member vo = new Member();

		vo.setId(userInfo.path("id").asText()); // id -> vo 넣기

		if (userInfo.path("kaccount_email_verified").asText().equals("true")) { // 이메일 받기 허용 한 경우
			vo.setId(userInfo.path("kaccount_email").asText()); // email -> vo 넣기
		} else { // 이메일 거부 할 경우 코드 추후 개발

		}

		JsonNode properties = userInfo.path("properties"); // 추가정보 받아오기
		if (properties.has("nickname"))
			vo.setNickname(properties.path("nickname").asText());
			vo.setImageUrl(properties.path("profile_image").asText());
		return vo;
	}
    @Override
    public String makeJwt(HttpServletRequest res) throws Exception {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Date expireTime = new Date();
        expireTime.setTime(expireTime.getTime() + 1000 * 60 * 1);
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        Map<String, Object> headerMap = new HashMap<String, Object>();

        headerMap.put("typ","JWT");
        headerMap.put("alg","HS256");

        Map<String, Object> map= new HashMap<String, Object>();

        String name = "김진호";
        String email = "kongkong@naver.com";

        map.put("name", name);
        map.put("email", email);

        JwtBuilder builder = Jwts.builder().setHeader(headerMap)
                .setClaims(map)
                .setExpiration(expireTime)
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
    @Override
    public String makeJwt(String idmember,String nickname, int time) throws Exception {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Date expireTime = new Date();
        expireTime.setTime(expireTime.getTime() + 1000 * 60 * 60 );
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        Map<String, Object> headerMap = new HashMap<String, Object>();

        headerMap.put("typ","JWT");
        headerMap.put("alg","HS256");

        Map<String, Object> map= new HashMap<String, Object>();

        map.put("idmember", idmember);
        map.put("nickname", nickname);

        JwtBuilder builder = Jwts.builder().setHeader(headerMap)
                .setClaims(map)
                .setExpiration(expireTime)
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
    
    @Override
    public String makeJwt(String idmember,int time) throws Exception {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Date expireTime = new Date();
        expireTime.setTime(expireTime.getTime() + 1000 * 60 * 60 );
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        Map<String, Object> headerMap = new HashMap<String, Object>();

        headerMap.put("typ","JWT");
        headerMap.put("alg","HS256");

        Map<String, Object> map= new HashMap<String, Object>();

        map.put("idmember", idmember);

        JwtBuilder builder = Jwts.builder().setHeader(headerMap)
                .setClaims(map)
                .setExpiration(expireTime)
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
    
    @Override
    public String makeJwt(String id, String pwd) throws Exception {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
        Date expireTime = new Date();
        expireTime.setTime(expireTime.getTime() + 1000 * 60 * 1);
        byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(secretKey);
        Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

        Map<String, Object> headerMap = new HashMap<String, Object>();

        headerMap.put("typ","JWT");
        headerMap.put("alg","HS256");

        Map<String, Object> map= new HashMap<String, Object>();

        map.put("id", id);
        map.put("password", pwd);

        JwtBuilder builder = Jwts.builder().setHeader(headerMap)
                .setClaims(map)
                .setExpiration(expireTime)
                .signWith(signatureAlgorithm, signingKey);

        return builder.compact();
    }
    
    @Override
    public boolean checkJwt(String jwt) throws Exception {
        try {
        	System.out.println("start");
            Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                    .parseClaimsJws(jwt).getBody(); // 정상 수행된다면 해당 토큰은 정상토큰
            System.out.println("Dd"+claims);
            logger.info("expireTime :" + claims.getExpiration());
            logger.info("name :" + claims.get("name"));
            return true;
        } catch (ExpiredJwtException exception) {
            logger.info("토큰 만료");
            return false;
        } catch (JwtException exception) {
            logger.info("토큰 변조");
            return false;
        }
    }
    
    
    @Override
    public int getIdmemberByToken(String jwt) throws Exception {
        try {
        	System.out.println("start");
            Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                    .parseClaimsJws(jwt).getBody(); // 정상 수행된다면 해당 토큰은 정상토큰
            System.out.println("Dd"+claims);
            logger.info("expireTime :" + claims.getExpiration());
            logger.info("name :" + claims.get("name"));
            return Integer.parseInt((String) claims.get("idmember"));
        } catch (ExpiredJwtException exception) {
            logger.info("토큰 만료");
            return -1;
        } catch (JwtException exception) {
            logger.info("토큰 변조");
            return -1;
        }
    }

	@Override
	public HttpStatus checkJwt2(String jwt) throws Exception {
		try {
        	System.out.println("start");
            Claims claims = Jwts.parser().setSigningKey(DatatypeConverter.parseBase64Binary(secretKey))
                    .parseClaimsJws(jwt).getBody(); // 정상 수행된다면 해당 토큰은 정상토큰
            System.out.println("Dd"+claims);
            logger.info("expireTime :" + claims.getExpiration());
            logger.info("idmember :" + claims.get("idmember"));
            return HttpStatus.ACCEPTED;
        } catch (ExpiredJwtException exception) {
            logger.info("토큰 만료");
            return HttpStatus.UNAUTHORIZED;
        } catch (JwtException exception) {
            logger.info("토큰 변조");
            return HttpStatus.UNAUTHORIZED;
        }
	}

	@Override
	public boolean getAccessTokenByRefreshToken(String tt) throws Exception {
		// TODO Auto-generated method stub
		return false;
	}
	
	@SuppressWarnings("unchecked")
    public Map<String, Object> getMapFromJsonObject( JSONObject jsonObj ) {
        Map<String, Object> map = null;
        try {
            map = new ObjectMapper().readValue(jsonObj.toJSONString(), Map.class);
            
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return map;
    }
	
	@Override
	public Tokens login(String id, String password) throws Exception {
		Member m = new Member();
		m.setId(id);
		m.setPassword(password);
		
		List<Member> list= memberDao.findMember(m);
		if(list.size()>0) 
		{
			m = list.get(0);
			m.setGrade("아이언");
			System.out.println("들어옴");
			String refreshToken = makeJwt(""+System.currentTimeMillis(),24*14);//나중에 뭘로 할지 찾기
			m.setRefreshToken(refreshToken);
			memberDao.updateRefreshToken(m);
			Tokens tokens = new Tokens(makeJwt(""+m.getIdmember(),m.getNickname(),1),refreshToken);
			return tokens;
		}
		else
		{
			System.out.println("안들어옴");
			return null;
		}
	}

	@Override
	public boolean isUsable(String token) throws Exception {
		HttpStatus status = checkJwt2(token);
		if(status==HttpStatus.ACCEPTED)
		{
			return true;
		}
		else return false;
	}

}
