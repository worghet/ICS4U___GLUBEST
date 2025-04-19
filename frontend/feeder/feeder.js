// == CONNECTION CONSTANTS ===========================
const FEEDER_API = "18.218.44.44:8090"; //
let SOCKET;


// == DOM CONSTANTS ==================================
const VIDEO_FEED_BOX = document.getElementById("video-feed-box");
const VIEWER_COUNT_ELEMENT = document.getElementById("viewer-count");
const LAST_FED_ELEMENT = document.getElementById("last-fed");
const click = new Audio("/resources/click.wav")
const meow = new Audio("/resources/meow.mp3")



// == ON LOAD / CLOSE ================================


// This method runs when the page is loaded.
window.onload = function () {

    // WEBSOCKET CONNECTION SETUP

    // Try to establish connection.
    try {
        setupWebsocketClient();
        console.log("SUCCESSFULLY CONNECTED TO: ", FEEDER_API);
    }

    // If anything goes wrong, let console know.
    catch (exception) {
        console.log("FAILED TO CONNECT TO: ", FEEDER_API);
    }

    // MUSIC (AUDIO) SETUP

    // Try to pull the saved time if exists (if not fallback on 0)
    const savedTime = parseFloat(localStorage.getItem("glubest_audio_time")) || 0;

    // Get the audio.
    const audio = document.getElementById("bg-music");

    // Adjust volume.
    audio.volume = 0.4;

    // If the audio was playing in the other menu.
    if (localStorage.getItem("bg-playing") == "true") {
        
        // Play the audio.
        audio.play();  

        // Set the time to the timestamp from leaving other page.
        audio.currentTime = savedTime;

    }
};

// What to do when we leave this page.
window.onclose = function () {

    // Disconnect the socket.
    SOCKET.close();

    // Save the time of the audio playing.
    localStorage.setItem("glubest_audio_time", audio.currentTime);

}


// == OTHER FUNCTIONS ========================================


function setupWebsocketClient() {

    // Setup socket connection.

    SOCKET = new WebSocket(`ws://${FEEDER_API}`);

    // Setup relevant methods.

    SOCKET.onopen = function (event) { };
    SOCKET.onclose = function (event) { };
    SOCKET.onerror = function (event) { };


    SOCKET.onmessage = function (event) {

        // Unpackage the data.
        let data = JSON.parse(event.data);
        console.log(data)

        // Identify each segment of the data.
        let imageBase64 = data.encodedImage;
        let viewerCount = data.viewerCount;
        let lastFedTime = data.formattedLastTimeFed;


        // Update corresponding HTML elements.
        VIDEO_FEED_BOX.innerHTML = "";
        VIDEO_FEED_BOX.style.backgroundImage = "url('data:image/png;base64," + imageBase64 + "')";
        VIEWER_COUNT_ELEMENT.innerText = viewerCount;
        LAST_FED_ELEMENT.innerText = lastFedTime;

    };

}

function sendFeedRequest() {

    // Play the meow sound.
    meow.play();

    // Send a simple HTTP request to the server, 
    // which then relays it to the Agent computer.

    fetch("/feed-request", { method: "POST" });

    // Notify console that it has been fulfilled.
    console.log("FEED REQUEST SENT.")

}

function backToMainMenu() {

    // Play the click sound.
    click.play();

    // Change the browser location to the main page.
    window.location.href = "/glubest";
}