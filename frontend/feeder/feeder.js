// == CONNECTION CONSTANTS ===========================
const FEEDER_API = "10.0.0.198:8090"; //18.218.44.44
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
    try {
        // Setup the actual connection.
        setupWebsocketClient();
        console.log("SUCCESSFULLY CONNECTED TO: ", FEEDER_API);
    } catch (exception) {
        console.log("FAILED TO CONNECT TO: ", FEEDER_API);
    }

    const audio = document.getElementById("bg-music");
    audio.volume = 0.4;

    const savedTime = parseFloat(localStorage.getItem("glubest_audio_time")) || 0;

   console.log(document.getElementById("bg-music"));

   
        console.log('mtdata loaded')
        if (localStorage.getItem("bg-playing") == "true") {
            audio.play();  // Start playing after setting the currentTime
            console.log(localStorage.getItem("glubest_audio_time"));

            audio.currentTime = savedTime;//localStorage.getItem("glubest_audio_time");
           
        }

    // Set the starting time when the audio starts playing
    setInterval(() => {
        console.log(audio.currentTime);
    }, 1000);
};


// audio.currentTime = 20;// localStorage.getItem("glubest_audio_time");  // Set the time before playing
// console.log(audio.currentTime);  // Should log '20'

window.onclose = function () {

    // On exiting / closing the page; disconnect the socket.
    SOCKET.close();

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

    meow.play();

    // Send a simple HTTP request to the server, 
    // which then relays it to the Agent computer.

    fetch("/feed-request", { method: "POST" });

    // Notify console that it has been fulfilled.
    console.log("FEED REQUEST SENT.")

}

function backToMainMenu() {

    click.play();

    // Just changes the location to the main page.
    window.location.href = "/glubest";
}