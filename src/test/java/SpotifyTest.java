import io.restassured.RestAssured;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import org.hamcrest.Matchers;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertThat;

public class SpotifyTest {

    private static final String TOKEN = "BQD9y5-GkzmRqFo2GM9LLoliOMr56iPreZxQzCv6yNr4Js4IT80VIKHC-6rCRDsoX1QWWDc96oOO4M_8jv93eA71PGUC74bziFM7XRnEZYh1jpJDYTkjAJWCg83WMh3o2WTDCvshpVK3CtvbcCrxV77i8I4qDNnJK3i6RhfTI3Sw4grszuh0y9V8UIya6JoSeCmsQxVOpAjiQVFkpif3DDST4gP0yrlBARlPXhftdM5ECr76WdByO4W_4-Zil-qZiLitpgBQP_N_y1ZGPcWxyYIy3A";

    @Test
    public void shouldCreatePlaylist() {

        // 1. Search and get track ids for creating a playlist
        List<String> trackIds = new ArrayList<>();
        Response searchResponse = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("q", "mfö")
                .queryParam("type", "track")
                .queryParam("market", "TR")
                .get("https://api.spotify.com/v1/search")
                .then()
                .statusCode(200)
                .extract()
                .response();

        //get 3 tracks from the track list
        JsonPath searchJsonPath = searchResponse.getBody().jsonPath();
        trackIds.add("spotify:track:" + searchJsonPath.getString("tracks.items[0].id"));
        trackIds.add("spotify:track:" + searchJsonPath.getString("tracks.items[1].id"));
        trackIds.add("spotify:track:" + searchJsonPath.getString("tracks.items[2].id"));

        // 2. Get current user ID
        Response currentUserResponse = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .get("https://api.spotify.com/v1/me")
                .then()
                .statusCode(200)
                .extract()
                .response();

        String currentUserId = currentUserResponse.getBody().jsonPath().getString("id");

        // 3. Create a playlist
        String createPlaylistRequest = "{\"name\":\"My API Test Playlistx\",\"description\":\"New playlist description\",\"public\":false}";

        Response createPlaylistResponse = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .body(createPlaylistRequest)
                .post("https://api.spotify.com/v1/users/" + currentUserId + "/playlists")
                .then()
                .statusCode(201)
                .extract()
                .response();

        String playlistId = createPlaylistResponse.getBody().jsonPath().getString("id");
        System.out.println(playlistId);

        //4.Check playlist is empty before adding any tracks
        Response checkemptyplaylist = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .get("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .then()
                .statusCode(200)
                .extract()
                .response();
        checkemptyplaylist.prettyPeek();

        // 5. Add tracks to created playlist
        Response addTracksResponse = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("uris", String.join(",", trackIds)) // spotify:track:123,spotify:track:1234
                .post("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .then()
                .statusCode(201)
                .extract()
                .response();

        // 6. Get created playlist tracks and chek them
        Response playlistResponse = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .get("https://api.spotify.com/v1/playlists/" + playlistId + "/tracks")
                .then()
                .statusCode(200)
                .extract()
                .response();

        playlistResponse.prettyPeek();

        // 7. Delete the last track from the playlist
        // benden bu kadar  :)

    }

    @Test
    public void shouldSearchGetArtistTracks() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("q", "mfö")
                .queryParam("type", "track")
                .queryParam("market", "TR")
                .get("https://api.spotify.com/v1/search")
                .then()
                .statusCode(200)
                .extract()
                .response();

        response.prettyPeek();
        assertThat(response.getBody().asString(), Matchers.containsString("Sarı Laleler"));
    }

    @Test
    public void shouldGetRecentlyPlayedTracks() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("limit", "2")
                .queryParam("after", "10")
                .get("https://api.spotify.com/v1/me/player/recently-played")
                .then()
                .statusCode(200)
                .extract().response();

        response.prettyPeek();
        assertThat(response.getBody().asString(), Matchers.containsString("The Animals"));
    }

    @Test
    public void shouldGetCurrentUsersSavedAlbums() {
        Response response = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("limit", "5")
                .queryParam("offset", "3")
                .queryParam("market", "TR")
                .get("https://api.spotify.com/v1/me/albums")
                .then()
                .statusCode(200)
                .extract().response();

        response.prettyPeek();  ////burda response'u console'a basarız.

        String name = response.getBody().jsonPath().getString("name");
        assertThat(name, Matchers.containsString("Adamlar"));
    }

    @Test
    public void shouldNotDeleteNonExistingAlbum() {
        Response responseDeleteAlbum = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("ids", "4inwzuD2iBZK8Ck3ft7Wlk")
                .delete("https://api.spotify.com/v1/me/albums")
                .then()
                .statusCode(400)
                .extract().response();

//        int statusCode = responseDeleteAlbum.getStatusCode();
//        Assert.assertEquals(statusCode,400);

        System.out.println(responseDeleteAlbum.getBody().jsonPath().getString("error"));
    }

    @Test
    public void shouldDeleteExistingAlbum() {
        Response responseDeleteAlbum = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("ids", "1nM08Vpgmq6ji4Bpng4sYW")
                .delete("https://api.spotify.com/v1/me/albums")
                .then()
                .statusCode(200)
                .extract().response();
        responseDeleteAlbum.prettyPeek();
    }

    @Test
    public void shouldGetAnAlbumWithId() {
        Response responseDeleteAlbum = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("ids", "1nM08Vpgmq6ji4Bpng4sYW")
                .delete("https://api.spotify.com/v1/me/albums")
                .then()
                .statusCode(200)
                .extract().response();
        //responseDeleteAlbum.prettyPeek();
        System.out.println(responseDeleteAlbum.jsonPath().prettyPrint());
    }

    @Test
    public void shouldDeleteTrackWithId() {
        Response responseDeleteTrack = RestAssured.given()
                .header("Authorization", "Bearer " + TOKEN)
                .queryParam("ids", "6Q4PYJtrq8CBx7YCY5IyRN")
                .delete("https://api.spotify.com/v1/me/tracks")
                .then()
                .statusCode(200)
                .extract().response();

        System.out.println(responseDeleteTrack.getBody().jsonPath().getString("href"));

    }


}
