export function formatPrice(price) {
    const value = Number(price);
    if (Number.isNaN(value)) {
        return '0.00';
    }
    return value.toFixed(2);
}

export function shuffle(list) {
    const array = [...list];
    for (let i = array.length - 1; i > 0; i -= 1) {
        const j = Math.floor(Math.random() * (i + 1));
        [array[i], array[j]] = [array[j], array[i]];
    }
    return array;
}
