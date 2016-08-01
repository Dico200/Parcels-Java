package com.redstoner.utils;

import com.redstoner.utils.Formatting;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class FormattingTest {
	
	@Test
	public void testFormatting() {

		assertEquals("§a", Formatting.from("4a").toString());
		assertEquals("§9", Formatting.from("123456789").toString());
		
		assertEquals("§2", Formatting.getFormats("&4Hello, &cM&2y& lname", '&').toString());
		assertEquals("§2§l", Formatting.getFormats("&4Hello, &cMy&2 &lname", '&').toString());
		
		assertEquals("§r", Formatting.CLEAR.toString());
		assertEquals("", Formatting.EMPTY.toString());
		
		assertEquals("§4this is dark §cthis is light §4this is dark again", 
				Formatting.translateChars('&', "&4this is dark &cthis is light &<this is dark again"));
	}
		

}
