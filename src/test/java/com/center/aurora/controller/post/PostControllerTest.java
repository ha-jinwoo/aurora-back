package com.center.aurora.controller.post;

import com.center.aurora.domain.post.Mood;
import com.center.aurora.domain.user.Role;
import com.center.aurora.domain.user.User;
import com.center.aurora.repository.user.UserRepository;
import com.center.aurora.security.TokenProvider;
import com.center.aurora.service.post.PostService;
import com.center.aurora.service.post.dto.PostDto;
import com.center.aurora.service.post.dto.PostResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PostControllerTest {
    @LocalServerPort
    private int port;

    @Autowired
    private PostService postService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TokenProvider tokenProvider;

    private MockMvc mvc;

    @BeforeEach
    public void setup(){
        mvc = MockMvcBuilders
                .webAppContextSetup(context)
                .addFilter(new CharacterEncodingFilter("UTF-8", true))
                .apply(springSecurity())
                .build();
        userRepository.deleteAll();
    }

    @DisplayName("게시물 생성")
    @Test
    public void createPost() throws Exception{
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);

        String url = "http://localhost:" + port + "/posts";
        String token = tokenProvider.createTokenByUserEntity(userA);

        //when
        mvc.perform(post(url)
                        .param("content", "content1")
                        .param("mood", "sun")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andDo(print());

        //then
        Pageable pageable = PageRequest.of(0, 5, Sort.Direction.DESC, "id");
        List<PostResponse> result = postService.getAllPost(pageable);

        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getMood()).isEqualTo(Mood.sun);
    }

    @DisplayName("모든 게시물 조회")
    @Test
    void getAllPost() throws Exception {
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        User userB = User.builder().name("B").email("b@b.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);
        userRepository.save(userB);

        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").build();
        PostDto postDto2 = PostDto.builder().mood(Mood.moon).content("content2").build();

        postService.createPost(userA.getId(), postDto);
        postService.createPost(userB.getId(), postDto2);

        //when
        String url = "http://localhost:" + port + "/posts/all?page=0";
        String token = tokenProvider.createTokenByUserEntity(userA);

        //then
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("특정 유저 게시물 조회")
    @Test
    void getPost() throws Exception {
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        User userB = User.builder().name("B").email("b@b.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);
        userRepository.save(userB);

        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").build();
        PostDto postDto2 = PostDto.builder().mood(Mood.moon).content("content2").build();

        postService.createPost(userA.getId(), postDto);
        postService.createPost(userB.getId(), postDto2);

        //when
        String url = "http://localhost:" + port + "/posts/" + userA.getId();

        //then
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("특정 게시물 조회")
    @Test
    void getOnePost() throws Exception {
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        User userB = User.builder().name("B").email("b@b.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);
        userRepository.save(userB);

        MultipartFile images = new MockMultipartFile(
                "test.png",
                "origin.png",
                MediaType.IMAGE_JPEG_VALUE,
                new FileInputStream("src/test/resources/images/image.jpg")
        );
        List<MultipartFile> imageList = new ArrayList<>();
        imageList.add(images);
        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").images(imageList).build();
        PostDto postDto2 = PostDto.builder().mood(Mood.moon).content("content2").images(imageList).build();

        postService.createPost(userA.getId(), postDto);
        postService.createPost(userB.getId(), postDto2);

        Pageable pageable = PageRequest.of(0, 5, Sort.Direction.DESC, "id");
        List<PostResponse> posts = postService.getPost(userA.getId(),pageable);

        //when
        String url = "http://localhost:" + port + "/posts/one/" + posts.get(0).getId();

        //then
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("날씨 별 게시물 조회")
    @Test
    void getAllPostByMood() throws Exception {
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        User userB = User.builder().name("B").email("b@b.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);
        userRepository.save(userB);

        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").build();
        PostDto postDto2 = PostDto.builder().mood(Mood.sun).content("content2").build();
        PostDto postDto3 = PostDto.builder().mood(Mood.rain).content("content3").build();
        PostDto postDto4 = PostDto.builder().mood(Mood.moon).content("content4").build();

        postService.createPost(userA.getId(), postDto);
        postService.createPost(userB.getId(), postDto2);
        postService.createPost(userA.getId(), postDto3);
        postService.createPost(userB.getId(), postDto4);

        //when
        String url = "http://localhost:" + port + "/posts/all/filter?mood=sun&page=0";

        //then
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("특정 유저 및 날씨 별 게시물 조회")
    @Test
    void getPostByUserAndMood() throws Exception {
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        User userB = User.builder().name("B").email("b@b.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);
        userRepository.save(userB);

        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").build();
        PostDto postDto2 = PostDto.builder().mood(Mood.sun).content("content2").build();
        PostDto postDto3 = PostDto.builder().mood(Mood.rain).content("content3").build();
        PostDto postDto4 = PostDto.builder().mood(Mood.moon).content("content4").build();

        postService.createPost(userA.getId(), postDto);
        postService.createPost(userB.getId(), postDto2);
        postService.createPost(userA.getId(), postDto3);
        postService.createPost(userB.getId(), postDto4);

        //when
        String url = "http://localhost:" + port + "/posts/"+userA.getId()+"/filter?mood=sun&page=0";

        //then
        mvc.perform(get(url))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @DisplayName("게시물 수정")
    @Test
    void updatePost() throws Exception{
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);

        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").build();

        postService.createPost(userA.getId(), postDto);

        Pageable pageable = PageRequest.of(0, 5, Sort.Direction.DESC, "id");
        List<PostResponse> result = postService.getPost(userA.getId(),pageable);

        assertThat(result.get(0).getAuth().getId()).isEqualTo(userA.getId());
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getMood()).isEqualTo(Mood.sun);

        //when
        String url = "http://localhost:" + port + "/posts/" + result.get(0).getId();
        String token = tokenProvider.createTokenByUserEntity(userA);

        mvc.perform(patch(url)
                        .param("content", "content2")
                        .param("mood", "moon")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andDo(print());

        //then
        result = postService.getPost(userA.getId(),pageable);

        assertThat(result.get(0).getContent()).isEqualTo("content2");
        assertThat(result.get(0).getMood()).isEqualTo(Mood.moon);
    }

    @DisplayName("게시물 삭제")
    @Test
    void deletePost() throws Exception{
        //given
        User userA = User.builder().name("A").email("a@a.com").image("").role(Role.USER).bio("").build();
        userRepository.save(userA);

        PostDto postDto = PostDto.builder().mood(Mood.sun).content("content1").build();

        postService.createPost(userA.getId(), postDto);

        Pageable pageable = PageRequest.of(0, 5, Sort.Direction.DESC, "id");
        List<PostResponse> result = postService.getPost(userA.getId(),pageable);

        assertThat(result.get(0).getAuth().getId()).isEqualTo(userA.getId());
        assertThat(result.get(0).getContent()).isEqualTo("content1");
        assertThat(result.get(0).getMood()).isEqualTo(Mood.sun);

        //when
        String url = "http://localhost:" + port + "/posts/" + result.get(0).getId();
        String token = tokenProvider.createTokenByUserEntity(userA);

        mvc.perform(delete(url)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andDo(print());

        //then
        result = postService.getPost(userA.getId(),pageable);

        assertThat(result.size()).isEqualTo(0);
    }
}
