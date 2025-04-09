const grid = document.getElementById("grid");
for (let i = 0; i < 110; i++) {
    const tile = document.createElement("div");
    tile.className = "tile";
    grid.appendChild(tile);
}

const character = document.getElementById("character");
let x = 0;
let y = 0;
const tileSize = 128;
let isMoving = false; // Prevent multiple movements when holding down key

function move(dx, dy) {
    const newX = x + dx;
    const newY = y + dy;

    // Optional: boundary check (0–9 grid)
    //   if (newX < 0 || newX >= 10 || newY < 0 || newY >= 10) return;

    x = newX;
    y = newY;
    character.style.transform = `translate(${x * tileSize}px, ${y * tileSize}px)`;
}

document.addEventListener("keydown", (e) => {
    if (!isMoving) {  // Only move if not already moving
        if (e.key === "ArrowUp") {
            move(0, -1);
        }
        if (e.key === "ArrowDown") {
            move(0, 1);
        }
        if (e.key === "ArrowLeft") {
            move(-1, 0);
        }
        if (e.key === "ArrowRight") {
            move(1, 0);
        }

        isMoving = true; // Set to true to prevent further movement during hold
    }
});

document.addEventListener("keyup", () => {
    isMoving = false; // Allow movement again once key is released
});
