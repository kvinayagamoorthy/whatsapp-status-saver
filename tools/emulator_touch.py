#!/usr/bin/env python3
"""Sends a double-tap, pinch or drag to the local emulator's touch screen.

`adb shell input` cannot pinch, and two `input tap` calls are too slow to count as a
double-tap, so this writes raw multi-touch events to the emulator's touch device
instead. Writing to the device needs root on the emulator:

    adb -s emulator-5554 root
    tools/emulator_touch.py dtap 540 1200
    tools/emulator_touch.py pinch 540 1200 100 400
    tools/emulator_touch.py drag 900 1200 200 1200
    adb -s emulator-5554 unroot

Coordinates are screen pixels. `pinch CX CY D0 D1` moves two fingers placed
horizontally around (CX, CY) from D0 to D1 pixels away from the centre: D1 > D0 zooms
in, D1 < D0 zooms out.

How it works: each gesture is a list of frames, each frame a group of 24-byte
`struct input_event` records ending in a SYN. One file per frame is pushed to the
device and written to the touch device with one `cat` per frame, with short sleeps in
between (`sendevent` takes about 20 ms per event, too slow for a double-tap). Each
touch-down sets ABS_MT_PRESSURE above zero; Android ignores touches without it.
"""

import argparse
import os
import struct
import subprocess
import sys
import tempfile

SERIAL = "emulator-5554"
TOUCH_DEVICE = "/dev/input/event2"  # virtio_input_multi_touch_1
DEVICE_DIR = "/data/local/tmp/emulator_touch"
SCREEN_WIDTH = 1080
SCREEN_HEIGHT = 2400
AXIS_MAX = 32767
FRAME_INTERVAL = 0.016
MOVE_STEPS = 15

EV_SYN = 0
EV_KEY = 1
EV_ABS = 3
BTN_TOUCH = 330
ABS_MT_TOUCH_MAJOR = 48
ABS_MT_SLOT = 47
ABS_MT_POSITION_X = 53
ABS_MT_POSITION_Y = 54
ABS_MT_TRACKING_ID = 57
ABS_MT_PRESSURE = 58
NO_TRACKING_ID = -1


def record(event_type, code, value):
    """One 64-bit `struct input_event`; the kernel fills in the time."""
    return struct.pack("<qqHHi", 0, 0, event_type, code, value)


def sync():
    return record(EV_SYN, 0, 0)


def move(slot, x, y):
    return (
        record(EV_ABS, ABS_MT_SLOT, slot)
        + record(EV_ABS, ABS_MT_POSITION_X, int(x * AXIS_MAX / SCREEN_WIDTH))
        + record(EV_ABS, ABS_MT_POSITION_Y, int(y * AXIS_MAX / SCREEN_HEIGHT))
    )


def finger_down(slot, tracking_id, x, y):
    return (
        record(EV_ABS, ABS_MT_SLOT, slot)
        + record(EV_ABS, ABS_MT_TRACKING_ID, tracking_id)
        + record(EV_ABS, ABS_MT_PRESSURE, 50)
        + record(EV_ABS, ABS_MT_TOUCH_MAJOR, 5)
        + move(slot, x, y)
    )


def finger_up(slot):
    return record(EV_ABS, ABS_MT_SLOT, slot) + record(EV_ABS, ABS_MT_TRACKING_ID, NO_TRACKING_ID)


def double_tap(x, y):
    """Returns the frames as (bytes, seconds to wait after the frame)."""
    frames = []
    for tap in range(2):
        frames.append((finger_down(0, 100 + tap, x, y) + record(EV_KEY, BTN_TOUCH, 1) + sync(), 0.03))
        frames.append((finger_up(0) + record(EV_KEY, BTN_TOUCH, 0) + sync(), 0.08))
    return frames


def pinch(center_x, center_y, start_distance, end_distance):
    frames = [
        (
            finger_down(0, 200, center_x - start_distance, center_y)
            + finger_down(1, 201, center_x + start_distance, center_y)
            + record(EV_KEY, BTN_TOUCH, 1)
            + sync(),
            FRAME_INTERVAL,
        )
    ]
    for step in range(1, MOVE_STEPS + 1):
        distance = start_distance + (end_distance - start_distance) * step / MOVE_STEPS
        frames.append(
            (
                move(0, center_x - distance, center_y) + move(1, center_x + distance, center_y) + sync(),
                FRAME_INTERVAL,
            )
        )
    frames.append((finger_up(0) + finger_up(1) + record(EV_KEY, BTN_TOUCH, 0) + sync(), 0))
    return frames


def drag(start_x, start_y, end_x, end_y):
    frames = [(finger_down(0, 300, start_x, start_y) + record(EV_KEY, BTN_TOUCH, 1) + sync(), FRAME_INTERVAL)]
    for step in range(1, MOVE_STEPS + 1):
        x = start_x + (end_x - start_x) * step / MOVE_STEPS
        y = start_y + (end_y - start_y) * step / MOVE_STEPS
        frames.append((move(0, x, y) + sync(), FRAME_INTERVAL))
    frames.append((finger_up(0) + record(EV_KEY, BTN_TOUCH, 0) + sync(), 0))
    return frames


def adb(*arguments):
    subprocess.run(["adb", "-s", SERIAL, *arguments], check=True, stdout=subprocess.DEVNULL)


def send(frames):
    """Pushes one file per frame plus a script that writes them in order, then runs it."""
    with tempfile.TemporaryDirectory() as local_dir:
        script = []
        for number, (data, pause) in enumerate(frames):
            name = f"{number:03d}.bin"
            with open(os.path.join(local_dir, name), "wb") as frame_file:
                frame_file.write(data)
            script.append(f"cat {DEVICE_DIR}/{name} > {TOUCH_DEVICE}")
            if pause:
                script.append(f"sleep {pause}")
        with open(os.path.join(local_dir, "run.sh"), "w") as script_file:
            script_file.write("\n".join(script) + "\n")
        adb("shell", "rm", "-rf", DEVICE_DIR)
        adb("shell", "mkdir", "-p", DEVICE_DIR)
        adb("push", "-q", f"{local_dir}/.", DEVICE_DIR)
    adb("shell", "sh", f"{DEVICE_DIR}/run.sh")


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    gestures = parser.add_subparsers(dest="gesture", required=True)
    gesture = gestures.add_parser("dtap", help="double-tap at X Y")
    gesture.add_argument("coordinates", nargs=2, type=float, metavar=("X", "Y"))
    gesture = gestures.add_parser("pinch", help="pinch around CX CY from D0 to D1 px")
    gesture.add_argument("coordinates", nargs=4, type=float, metavar=("CX", "CY", "D0", "D1"))
    gesture = gestures.add_parser("drag", help="one-finger drag from X0 Y0 to X1 Y1")
    gesture.add_argument("coordinates", nargs=4, type=float, metavar=("X0", "Y0", "X1", "Y1"))
    arguments = parser.parse_args()

    builders = {"dtap": double_tap, "pinch": pinch, "drag": drag}
    send(builders[arguments.gesture](*arguments.coordinates))
    return 0


if __name__ == "__main__":
    sys.exit(main())
