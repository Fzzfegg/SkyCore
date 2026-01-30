package org.mybad.minecraft.client.input;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;
import org.mybad.minecraft.SkyCoreMod;

@SideOnly(Side.CLIENT)
public final class HiddenReloadHotkey {

    private boolean wasComboPressed;

    @SubscribeEvent
    public void onKeyInput(InputEvent.KeyInputEvent event) {
        boolean altDown = Keyboard.isKeyDown(Keyboard.KEY_LMENU) || Keyboard.isKeyDown(Keyboard.KEY_RMENU);
        boolean pDown = Keyboard.isKeyDown(Keyboard.KEY_P);
        boolean combo = altDown && pDown;
        if (combo && !wasComboPressed) {
            wasComboPressed = true;
            triggerReload();
        } else if (!combo) {
            wasComboPressed = false;
        }
    }

    private void triggerReload() {
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            SkyCoreMod.instance.reload();
            mc.player.sendMessage(new TextComponentString("SkyCore 全局资源已重新加载"));
        });
    }
}
