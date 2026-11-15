import assert from 'node:assert/strict';
import { buildChatGPTLaunchUrl } from './main';

const prompt = 'Explain Pythagoras theorem for a right triangle.';
const url = new URL(buildChatGPTLaunchUrl(prompt));
assert.equal(url.origin, 'https://chatgpt.com');
assert.equal(url.searchParams.get('q'), prompt);
console.log('guided ChatGPT launch URL test passed');
