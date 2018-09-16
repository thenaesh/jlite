class Arithmetic {
  Void main(Float x, Float y, Float r, Float theta) {
    Float epsilon;
    epsilon = 0;
    return abs(r * (cos(theta) + i * sin(theta)) - (x + i * y)) < this.epsilon;
  }
}
