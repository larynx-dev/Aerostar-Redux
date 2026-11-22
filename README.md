# AeroStar Redux

**Fly your Create: Aeronautics ships to the planets of Create: Northstar Redux.**

AeroStar Redux is a compatibility addon for **Create: Northstar Redux** and **Create: Aeronautics**. You build a physics ship, research planets with Northstar's Space Atlas, and then fly the whole ship (with everyone on board) to Earth Orbit, the Moon, Mars, Venus or Mercury using the **Dimensional Drive**.

---

## Features

### Dimensional Drive
The Dimensional Drive is a block that moves an entire Aeronautics ship to another dimension.

- Place it on a physics ship and insert a **Space Atlas**.
- The drive reads the planets you have researched in that atlas. Click the destination label in the GUI to cycle through the ones you've unlocked.
- Arm the drive, then climb. Once the ship passes the required altitude it is moved to the destination, crew and cargo included.
- With Engineer's Goggles you can see the drive's destination, the ship's speed, its current altitude and the altitude it needs to reach.

**Altitude needed to launch from the Overworld:**

| Destination | Altitude |
|-------------|----------|
| Earth Orbit | 1750 |
| Moon        | 1750 |
| Mars        | 2200 |
| Venus       | 2600 |
| Mercury     | 3000 |

Launching from any other dimension needs an altitude of 1750.

### Guided progression
- A destination appears in the drive only after you have gathered enough **science** for it in the Space Atlas, the same rule Northstar's Rocket Station uses.
- To reach **Mars, Venus or Mercury**, the ship also needs an **Interplanetary Navigator** on board. The Moon, Earth Orbit and Earth don't.

### Orbital Physics Assembler
The Orbital Physics Assembler is a Physics Assembler that can **disassemble a ship in mid-air**. You need this for building stations and docking in places where there is no ground.

### Planet physics
- Each Northstar planet has its own ship gravity (Moon, Mars, Venus, Mercury).
- **Earth Orbit** cancels gravity for ships, so they float freely.
- If a ship drops below Y 20 in Earth Orbit, it re-enters the Overworld. A player who falls out of orbit without a ship is returned above the world spawn.

### Admin command
`/drivetransfer <planet>` (op level 2) immediately sends the ship you are standing on to a planet. Valid planets are `earth_orbit`, `earth`, `moon`, `mars`, `venus` and `mercury`. It's useful for testing and for servers.

---

## Crafting

| Block | Recipe |
|-------|--------|
| **Dimensional Drive** | Mechanical Crafting (5×5): titanium ingots and plates, Northstar circuits, polished lunar sapphires, precision mechanisms and a targeting computer |
| **Orbital Physics Assembler** | Crafting table: a Physics Assembler surrounded by titanium plates, precision mechanisms and Northstar circuits |

Use JEI or EMI to see the exact layouts.

---

## Requirements

| Mod | Version |
|-----|---------|
| Minecraft | 1.21.1 |
| NeoForge | 21.1.233 or newer |
| Create | 6.0.0 or newer |
| Create: Northstar Redux | 0.5.0 or newer (tested with 0.6.6) |
| Create: Aeronautics (bundled, includes Simulated) | 1.0.0 or newer |
| Sable | 1.2.0 or newer |
| GeckoLib | required by Northstar |

Install the mod on **both the client and the server**.

---

## How to use

1. Build a ship with Create: Aeronautics and assemble it.
2. Research a planet in Northstar until your Space Atlas has enough science to unlock it.
3. Place a **Dimensional Drive** on the ship and put the Space Atlas in its slot.
4. Click the destination label to pick a planet, then **arm** the drive.
5. For Mars, Venus or Mercury, make sure there is an **Interplanetary Navigator** on the ship.
6. Fly upward. When the ship passes the required altitude it is sent to the destination.

---

## Building from source

```sh
git clone https://github.com/larynx-dev/Aerostar-Redux.git
cd Aerostar-Redux
./gradlew build
```

The jar is written to `build/libs/`. Use `./gradlew runClient` to start a development client.

---

## Credits

- **Larynx**, maintainer of AeroStar Redux
- **JozelbaBega (BEGA)**, author of AeroStar
- **Vladi-YT-Dev**, author of the original *Create: Northstar-Aeronautics Compatibility*

AeroStar Redux is a fork of AeroStar, which is itself based on Create: Northstar-Aeronautics Compatibility.

## License

[MIT](LICENSE). The original copyright notices are kept in the LICENSE file.
