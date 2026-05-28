package com.Project.App.Multipong;

import java.util.UUID;

final class BtConstants {
    private BtConstants() {}

    // Fixed RFCOMM service UUID shared by all MultiPong instances
    static final UUID MULTIPONG_UUID = UUID.fromString("a8a74545-e483-4cbb-b54c-5c4e7b6cfbc3");
}
